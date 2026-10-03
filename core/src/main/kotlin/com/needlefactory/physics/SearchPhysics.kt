package com.needlefactory.physics

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.needlefactory.content.SearchConfig
import com.needlefactory.simulation.SearchPiece
import com.needlefactory.simulation.SearchRound
import kotlin.math.*

/** Owns native resources and publishes pose snapshots into the domain round. */
class SearchPhysics(private val round: SearchRound, private val config: SearchConfig) : AutoCloseable {
    private val world: World
    private val clutter: List<Body>
    private val needle: Body
    private val pusher: Body
    var pushing = false
        private set
    private var targetX = 0f
    private var targetY = 0f
    val bodyCount: Int get() = world.bodyCount
    var containmentCorrections = 0
        private set
    val pusherX: Float get() = pusher.position.x
    val pusherY: Float get() = pusher.position.y

    init {
        Box2D.init()
        world = World(Vector2.Zero, true)
        wall(-0.25f, config.trayHeight / 2f, 0.25f, config.trayHeight / 2f + 0.5f)
        wall(config.trayWidth + 0.25f, config.trayHeight / 2f, 0.25f, config.trayHeight / 2f + 0.5f)
        wall(config.trayWidth / 2f, -0.25f, config.trayWidth / 2f, 0.25f)
        wall(config.trayWidth / 2f, config.trayHeight + 0.25f, config.trayWidth / 2f, 0.25f)
        clutter = round.clutter.map { dynamic(it, false) }
        needle = dynamic(round.needle, true)
        pusher = world.createBody(BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody; bullet = true; fixedRotation = true
        })
        val circle = CircleShape().apply { radius = config.pusherRadius }
        try {
            pusher.createFixture(FixtureDef().apply {
                shape = circle; density = config.pusherDensity; friction = 0.25f; restitution = 0f
                filter.categoryBits = 8; filter.maskBits = 7
            })
        } finally { circle.dispose() }
        pusher.isActive = false
    }

    private fun wall(x: Float, y: Float, hx: Float, hy: Float) {
        val body = world.createBody(BodyDef().apply { position.set(x, y) })
        val box = PolygonShape().apply { setAsBox(hx, hy) }
        try {
            body.createFixture(FixtureDef().apply {
                shape = box; friction = 0.25f
                filter.categoryBits = 1; filter.maskBits = 14
            })
        } finally { box.dispose() }
    }

    private fun dynamic(piece: SearchPiece, isNeedle: Boolean): Body {
        val body = world.createBody(BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            position.set(piece.x, piece.y); angle = piece.angle
            linearDamping = if (isNeedle) config.needleDamping else config.clutterDamping
            angularDamping = if (isNeedle) config.needleDamping else config.angularDamping
            bullet = true
        })
        val box = PolygonShape().apply {
            setAsBox(piece.length / 2f * config.fixtureScale, piece.width / 2f * config.fixtureScale)
        }
        try {
            body.createFixture(FixtureDef().apply {
                shape = box; density = if (isNeedle) config.needleDensity else config.clutterDensity
                friction = 0.25f; restitution = 0f
                filter.categoryBits = if (isNeedle) 4 else 2
                filter.maskBits = 9 // Walls + pusher only; bulk has no pairwise contacts.
            })
        } finally { box.dispose() }
        return body
    }

    fun begin(x: Float, y: Float) {
        if (round.found) return
        target(x, y)
        pusher.setTransform(targetX, targetY, 0f)
        pusher.setLinearVelocity(0f, 0f)
        pusher.isActive = true
        pushing = true
    }

    fun target(x: Float, y: Float) {
        targetX = x.coerceIn(config.pusherRadius, config.trayWidth - config.pusherRadius)
        targetY = y.coerceIn(config.pusherRadius, config.trayHeight - config.pusherRadius)
    }

    fun end() {
        pushing = false
        pusher.setLinearVelocity(0f, 0f)
        pusher.isActive = false
    }

    fun step() {
        for (i in clutter.indices) remember(round.clutter[i])
        remember(round.needle)
        if (pushing) {
            val dx = targetX - pusher.position.x; val dy = targetY - pusher.position.y
            val distance = hypot(dx, dy)
            val scale = if (distance <= 0.0001f) 0f else min(config.pusherSpeed, distance / config.stepSeconds) / distance
            pusher.setLinearVelocity(dx * scale, dy * scale)
        }
        world.step(config.stepSeconds, 6, 2)
        for (i in clutter.indices) publish(clutter[i], round.clutter[i], config.maxClutterSpeed)
        publish(needle, round.needle, config.maxNeedleSpeed)
    }

    private fun remember(p: SearchPiece) {
        p.previousX = p.x; p.previousY = p.y; p.previousAngle = p.angle
    }

    private fun publish(body: Body, p: SearchPiece, maxSpeed: Float) {
        val v = body.linearVelocity
        if (v.len2() > maxSpeed * maxSpeed) {
            val scale = maxSpeed / v.len()
            body.setLinearVelocity(v.x * scale, v.y * scale)
        }
        body.angularVelocity = body.angularVelocity.coerceIn(-config.maxAngularSpeed, config.maxAngularSpeed)
        // Visible strips are larger than fixtures. Enforce the tray's visual
        // envelope as a final constraint, including initial pusher overlaps at
        // a wall. Projection cancels outward velocity and never injects energy.
        val a = body.angle
        val hx = (abs(cos(a)) * p.length + abs(sin(a)) * p.width) / 2f + 0.02f
        val hy = (abs(sin(a)) * p.length + abs(cos(a)) * p.width) / 2f + 0.02f
        val x = body.position.x; val y = body.position.y
        val cx = x.coerceIn(hx, config.trayWidth - hx)
        val cy = y.coerceIn(hy, config.trayHeight - hy)
        if (x != cx || y != cy) {
            containmentCorrections++
            body.setTransform(cx, cy, a)
            val velocity = body.linearVelocity
            body.setLinearVelocity(if (x != cx) 0f else velocity.x, if (y != cy) 0f else velocity.y)
            body.angularVelocity = 0f
        }
        p.x = body.position.x; p.y = body.position.y; p.angle = body.angle
    }

    /** Prevent a stale interpolation tail when lifecycle/input is interrupted. */
    fun synchronizeSnapshots() {
        for (p in round.clutter) remember(p)
        remember(round.needle)
    }

    override fun close() { world.dispose() }
}
