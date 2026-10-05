package com.needlefactory.physics

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.needlefactory.simulation.*
import kotlin.math.*

/** Bounded intake only. Each tuft owns one body with three gently curved collision sections. */
class FactoryIntakePhysics(private val session: FactorySession) : AutoCloseable {
    private val config = session.config
    private val world: World
    private val bodies: Array<Body?>
    private val pusher: Body
    private var targetX = 0f; private var targetY = 0f
    val bodyCount: Int get() = world.bodyCount
    val pushing: Boolean get() = pusher.isActive
    init {
        Box2D.init()
        world = World(Vector2.Zero, true)
        bodies = arrayOfNulls(session.batch.items.size)
        for (item in session.batch.items) if (item.route == Route.INTAKE) {
            val body = world.createBody(BodyDef().apply {
                type = BodyDef.BodyType.DynamicBody
                position.set(item.x, item.y); angle = item.angle
                linearDamping = 4f; angularDamping = 10f; bullet = true
            })
            val shape = PolygonShape()
            val sections = if (item.material == Material.STRAW) 3 else 1
            for (s in 0 until sections) {
                val u = (s + .5f) / sections - .5f
                shape.setAsBox(item.length / sections / 2f,
                    if (item.material == Material.STRAW) .21f else .035f,
                    Vector2(u * item.length, item.bend * (1 - 4 * u * u)), atan(-8 * item.bend * u / item.length))
                body.createFixture(FixtureDef().apply {
                    this.shape = shape; density = if (item.material == Material.NEEDLE) 3f else .6f
                    friction = .2f
                    filter.categoryBits = 1; filter.maskBits = 2 // Tufts overlap; only the physical finger displaces them.
                })
            }
            shape.dispose()
            body.setLinearVelocity(item.vx, item.vy); body.angularVelocity = item.angularVelocity
            body.userData = item
            bodies[item.id] = body
        }
        pusher = world.createBody(BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody; fixedRotation = true; bullet = true; active = false
        })
        val circle = CircleShape().apply { radius = config.pusherRadius }
        pusher.createFixture(FixtureDef().apply {
            shape = circle; density = 10f; filter.categoryBits = 2; filter.maskBits = 1
        })
        circle.dispose()
        world.setContactListener(object: ContactListener {
            override fun beginContact(contact: Contact) = Unit
            override fun endContact(contact: Contact) = Unit
            override fun postSolve(contact: Contact, impulse: ContactImpulse) = Unit
            override fun preSolve(contact: Contact, oldManifold: Manifold) {
                if (!pusher.isActive || pusher.linearVelocity.x <= .25f) return
                val a=contact.fixtureA.body; val b=contact.fixtureB.body
                val item=(if(a===pusher) b.userData else if(b===pusher) a.userData else null) as? FactoryItem
                if(item!=null) session.manualPush(item.id)
            }
        })
    }
    fun begin(x: Float, y: Float) {
        target(x, y); pusher.setTransform(targetX, targetY, 0f)
        pusher.setLinearVelocity(0f, 0f); pusher.isActive = true
    }
    fun target(x: Float, y: Float) {
        targetX = x.coerceIn(config.pusherRadius, config.width - config.pusherRadius)
        targetY = y.coerceIn(config.pusherRadius, config.height - config.pusherRadius)
    }
    fun end() { pusher.isActive = false }
    fun step() {
        if (!session.running || session.batch.complete) return
        val dt = config.stepSeconds
        val feeding = session.operating(MachineId.VIBRATION) && session.operating(MachineId.CONVEYOR)
        // No directional force, conveyor suction or scripted intake travel. Only finger contact
        // imparts motion. Damping settles released material; the belt accepts it at the real edge.
        if (pusher.isActive) {
            val dx = (targetX - pusher.position.x) / dt; val dy = (targetY - pusher.position.y) / dt
            val scale = min(1f, config.pusherSpeed / max(.001f, hypot(dx, dy)))
            pusher.setLinearVelocity(dx * scale, dy * scale)
        }
        world.step(dt, 6, 2)
        for (item in session.batch.items) {
            val body = bodies[item.id] ?: continue
            // Entire rendered strand envelope remains on the table, even after a fast swipe.
            val c=cos(body.angle); val s=sin(body.angle)
            val ex = item.extent(c,s,true)
            val ey = item.extent(c,s,false)
            val x = body.position.x.coerceIn(ex, config.width - ex)
            val y = body.position.y.coerceIn(ey, config.height - ey)
            if (x != body.position.x || y != body.position.y) body.setTransform(x, y, body.angle)
            val v = body.linearVelocity
            val scale = min(1f, 7f / max(.001f, v.len()))
            body.setLinearVelocity(v.x * scale, v.y * scale)
            body.angularVelocity = body.angularVelocity.coerceIn(-2.5f, 2.5f)
            item.x = x; item.y = y; item.angle = body.angle
            item.vx = body.linearVelocity.x; item.vy = body.linearVelocity.y; item.angularVelocity = body.angularVelocity
            // Gate opens to each item at its own physical edge, not an off-screen timer teleport.
            if (feeding && item.manuallyPushed && x >= config.width - ex - .05f && session.gateFeed(item.id)) {
                world.destroyBody(body); bodies[item.id] = null
            }
        }
    }
    override fun close() { world.dispose() }
}
