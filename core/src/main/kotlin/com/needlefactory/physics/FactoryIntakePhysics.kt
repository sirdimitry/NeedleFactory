package com.needlefactory.physics

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.badlogic.gdx.physics.box2d.joints.MouseJoint
import com.badlogic.gdx.physics.box2d.joints.MouseJointDef
import com.needlefactory.simulation.*
import com.needlefactory.content.FactoryConfig
import kotlin.math.*
import com.needlefactory.economy.*

/** Bounded intake only. Each tuft owns one body with three gently curved collision sections. */
class FactoryIntakePhysics(private val session: FactorySession) : AutoCloseable {
    private val config = session.config
    private var installedDividers=emptyList<FieldEquipment>()
    private var installedEquipment:List<FieldEquipment>?=null
    private val dividers=mutableListOf<Body>()
    private var robotId=-1
    private var robotJoint:MouseJoint?=null
    val robotCarrying get()=robotId>=0
    private var robotGripAge=0f
    /** Timed grasp phases keep fingertip contact ahead of transport, and release ahead of return. */
    val robotGripProgress get()=if(robotCarrying) (robotGripAge/.16f).coerceIn(0f,1f)
        else (1-robotReleaseAge/.18f).coerceIn(0f,1f)
    var robotReleaseAge=10f; private set
    var robotX=7.5f; private set
    var robotY=-2f; private set
    private val robotTarget=Vector2()
    private val powered=BooleanArray(session.config.strawCount+session.config.needleCount)
    private val world: World
    private val bodies: Array<Body?>
    private val pusher: Body
    private var ground:Body?=null
    private var grab:MouseJoint?=null
    private val extraGrips=arrayOfNulls<MouseJoint>(config.strawCount-1)
    private val extraIds=IntArray(config.strawCount-1) { -1 }
    private val extraOffsets=Array(config.strawCount-1) { Vector2() }
    private val extraTarget=Vector2()
    var grabbedId:Int=-1; private set
    private val grabTarget=Vector2()
    private val carried= arrayOfNulls<MouseJoint>(config.needleCount)
    private val carryOffsets=Array(config.needleCount) { Vector2() }
    private val carrySources=IntArray(config.needleCount) { -1 }
    private val carryTarget=Vector2()
    private var targetX = 0f; private var targetY = 0f
    val bodyCount: Int get() = world.bodyCount
    val pushing: Boolean get() = if(config.grabClumps) grab!=null else pusher.isActive
    val pusherX: Float get() = if(config.grabClumps) targetX else pusher.position.x
    val pusherY: Float get() = if(config.grabClumps) targetY else pusher.position.y
    private val radiusY = if(session.rake) config.pusherRadius*1.8f else config.pusherRadius
    private val radiusX = if(session.rake) .24f else config.pusherRadius
    private val localContacts = IntArray(config.rakeContactBudget)
    val heldCount:Int get()=(if(grab!=null) 1 else 0)+extraGrips.count { it!=null }
    private fun isHeld(id:Int)=id==grabbedId && grab!=null || extraIds.indices.any { extraIds[it]==id && extraGrips[it]!=null }
    private fun releaseGrip(id:Int) {
        if(id==grabbedId) {
            grab?.let { world.destroyJoint(it) }; grab=null; grabbedId=-1
            // Promote an existing local grip without moving its anchor or granting new contact.
            val slot=extraGrips.indexOfFirst { it!=null }
            if(slot>=0) {
                grab=extraGrips[slot]; grabbedId=extraIds[slot]
                primaryOffset.set(extraOffsets[slot]); extraGrips[slot]=null; extraIds[slot]=-1
            }
        } else for(i in extraIds.indices) if(extraIds[i]==id) {
            extraGrips[i]?.let { world.destroyJoint(it) }; extraGrips[i]=null; extraIds[i]=-1
        }
        for(i in carried.indices) if(carrySources[i]==id) detachMetal(i)
    }
    private val primaryOffset=Vector2()
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
            if(config.grabClumps && item.material==Material.STRAW) {
                for(fi in 0 until item.collisionStrandCount) for(section in 0..2) {
                    val f=item.strands[fi]
                    val u=(section+.5f)/3-.5f
                    val y=f.bend*(1-4*u*u)
                    shape.setAsBox(f.length/6f,f.width/2,
                        Vector2(f.x+f.cosine*u*f.length-f.sine*y,f.y+f.sine*u*f.length+f.cosine*y),
                        f.angle+atan(-8*f.bend*u/f.length))
                    body.createFixture(FixtureDef().apply {
                        this.shape=shape; density=.6f; friction=.15f
                        filter.categoryBits=1; filter.maskBits=10
                    })
                }
            } else {
            val sections = if (item.material == Material.STRAW) 3 else 1
            for (s in 0 until sections) {
                val u = (s + .5f) / sections - .5f
                shape.setAsBox(item.length / sections / 2f,
                    if (item.material == Material.STRAW) .21f else .035f,
                    Vector2(u * item.length, item.bend * (1 - 4 * u * u)), atan(-8 * item.bend * u / item.length))
                body.createFixture(FixtureDef().apply {
                    this.shape = shape; density = if (item.material == Material.NEEDLE) 3f else .6f
                    friction = .2f
                    filter.categoryBits = 1; filter.maskBits = if(config.grabClumps) 8 else if(session.guides) 6 else 2
                })
            }
            }
            shape.dispose()
            body.setLinearVelocity(item.vx, item.vy); body.angularVelocity = item.angularVelocity
            body.userData = item
            bodies[item.id] = body
        }
        if(session.guides && !config.grabClumps) for(top in listOf(false,true)) {
            val body=world.createBody(BodyDef())
            val y=if(top) config.height else 0f; val inward=if(top) -.65f else .65f
            val guide=PolygonShape().apply { set(floatArrayOf(config.width-2f,y,config.width,y,config.width,y+inward)) }
            body.createFixture(FixtureDef().apply {
                shape=guide; friction=.08f; filter.categoryBits=4; filter.maskBits=1
            }); guide.dispose()
        }
        pusher = world.createBody(BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody; fixedRotation = true; bullet = true; active = false
        })
        // Shallow bowed head deflects locally instead of bulldozing a rectangular band.
        val circle: Shape = if(session.rake) PolygonShape().apply {
            set(floatArrayOf(-radiusX,-radiusY,radiusX*.35f,-radiusY,radiusX,0f,
                radiusX*.35f,radiusY,-radiusX,radiusY))
        }
            else CircleShape().apply { radius = config.pusherRadius }
        pusher.createFixture(FixtureDef().apply {
            shape = circle; density = 10f; filter.categoryBits = 2; filter.maskBits = 1
        })
        circle.dispose()
        if(config.grabClumps) ground=world.createBody(BodyDef())
        world.setContactListener(object: ContactListener {
            override fun beginContact(contact: Contact) = Unit
            override fun endContact(contact: Contact) = Unit
            override fun postSolve(contact: Contact, impulse: ContactImpulse) = Unit
            override fun preSolve(contact: Contact, oldManifold: Manifold) {
                if (!pusher.isActive) return
                val a=contact.fixtureA.body; val b=contact.fixtureB.body
                val item=(if(a===pusher) b.userData else if(b===pusher) a.userData else null) as? FactoryItem
                if(item!=null) session.manualPush(item.id)
            }
        })
    }
    fun begin(x: Float, y: Float, tolerance:Float=config.grabRadius) {
        if(config.grabClumps) {
            end(); target(x,y)
            val hit=StrawGrab.pick(session.batch,x,y,tolerance,config.physicalNeedles) ?: return
            val body=bodies[hit.id] ?: return
            val metal=session.batch.items[hit.id].material==Material.NEEDLE
            grabbedId=hit.id
            for(fixture in body.fixtureList) {
                // Overlapping stalks form a layered pile, not an impossible flat rigid knot.
                // A held clump skims the top layer; bounded local crowding supplies resistance.
                val filter=fixture.filterData; filter.categoryBits=2; filter.maskBits=8; fixture.filterData=filter
            }
            // Attach at the actual touched stalk, preserving the initial grab offset.
            grab=world.createJoint(MouseJointDef().apply {
                bodyA=ground; bodyB=body; target.set(hit.x,hit.y)
                maxForce=body.mass*if(metal) config.needleGrabAcceleration else config.grabAcceleration*session.handSpeed
                frequencyHz=if(metal) config.needleGrabFrequency else (config.grabFrequency*sqrt(session.handSpeed)).coerceAtMost(20f); dampingRatio=.9f
            }) as MouseJoint
            primaryOffset.setZero()
            targetX=hit.x; targetY=hit.y; body.isAwake=true
            body.linearDamping=4f
            if(!metal && session.handCapacity>1) {
                // Only stalks within the bought head at touch-down, never later remote pickup.
                val candidates=ArrayList<StrawGrab.Hit>()
                for(item in session.batch.items) if(item.material==Material.STRAW && item.id!=hit.id && item.route==Route.INTAKE) {
                    val local=StrawGrab.pick(FactoryBatch(session.batch.seed,listOf(item)),hit.x,hit.y,session.handReach)
                    if(local!=null) candidates.add(local)
                }
                candidates.sortBy { it.distance }
                for(i in 0 until minOf(session.handCapacity-1,candidates.size)) {
                    val other=candidates[i]; val b=bodies[other.id] ?: continue
                    extraIds[i]=other.id; extraOffsets[i].set(other.x-hit.x,other.y-hit.y)
                    for(fixture in b.fixtureList) {
                        val filter=fixture.filterData; filter.categoryBits=2; filter.maskBits=8; fixture.filterData=filter
                    }
                    extraGrips[i]=world.createJoint(MouseJointDef().apply {
                        bodyA=ground; bodyB=b; target.set(other.x,other.y)
                        maxForce=b.mass*config.grabAcceleration*session.handSpeed; frequencyHz=(config.grabFrequency*sqrt(session.handSpeed)).coerceAtMost(20f); dampingRatio=.9f
                    }) as MouseJoint
                    b.isAwake=true; b.linearDamping=4f
                }
            }
            if(config.physicalNeedles && !metal) {
                attachLocalMetal(body,hit.id)
                for(i in extraIds.indices) if(extraIds[i]>=0) bodies[extraIds[i]]?.let { attachLocalMetal(it,extraIds[i]) }
            }
            return
        }
        session.clearManualPushes()
        for(body in bodies) body?.linearDamping=4f
        target(x, y); pusher.setTransform(targetX, targetY, 0f)
        pusher.setLinearVelocity(0f, 0f); pusher.isActive = true
    }
    fun target(x: Float, y: Float) {
        if(config.grabClumps) {
            val metal=grabbedId>=0 && session.batch.items[grabbedId].material==Material.NEEDLE
            targetX=x.coerceIn(0f,config.width+.04f); targetY=y.coerceIn(if(config.pitReceiver) config.intakeMinY else if(metal) -.85f else 0f,config.height)
            grab?.setTarget(grabTarget.set(targetX+primaryOffset.x,targetY+primaryOffset.y))
            for(i in extraGrips.indices) extraGrips[i]?.setTarget(extraTarget.set(targetX+extraOffsets[i].x,targetY+extraOffsets[i].y))
            return
        }
        targetX = x.coerceIn(radiusX, config.width - radiusX)
        targetY = y.coerceIn(radiusY, config.height - radiusY)
    }
    fun end() {
        // Releasing above the real opening commits locally carried material even if
        // the sequential gate had not processed all grips. No idle/world attraction.
        if(config.pitReceiver && grab!=null && session.receivingEnabled && insidePit(targetX,targetY)) {
            val ids=intArrayOf(grabbedId)+extraIds.filter { it>=0 }
            for(id in ids) {
                val b=bodies.getOrNull(id) ?: continue
                if(!insidePit(b.position.x,b.position.y)) continue
                session.manualPush(id)
                if(session.feed(id)) {
                    acceptLocalMetal(id)
                    val item=session.batch.items[id];item.x=b.position.x;item.y=b.position.y;item.angle=b.angle
                }
            }
        }
        if(config.grabClumps) {
            for(i in carried.indices) detachMetal(i)
            grab?.let { world.destroyJoint(it) }; grab=null
            if(grabbedId>=0) bodies[grabbedId]?.let { body ->
                for(fixture in body.fixtureList) {
                    val filter=fixture.filterData; filter.categoryBits=1; filter.maskBits=10; fixture.filterData=filter
                }
            }
            for(i in extraGrips.indices) {
                extraGrips[i]?.let { world.destroyJoint(it) }; extraGrips[i]=null
                bodies.getOrNull(extraIds[i])?.let { b -> for(f in b.fixtureList) {
                    val filter=f.filterData; filter.categoryBits=1; filter.maskBits=10; f.filterData=filter
                } }
                extraIds[i]=-1
            }
            grabbedId=-1
        }
        pusher.setLinearVelocity(0f,0f); pusher.isActive = false
        session.clearManualPushes()
        for(body in bodies) body?.linearDamping=config.releasedDamping
    }
    fun step() {
        if (!session.running || session.phase == BatchPhase.RESULTS) return
        val dt = config.stepSeconds
        val feeding = session.receivingEnabled
        session.clearManualPushes()
        // No directional force, conveyor suction or scripted intake travel. Only finger contact
        // imparts motion. Damping settles released material; the belt accepts it at the real edge.
        if (pusher.isActive) {
            val dx = (targetX - pusher.position.x) / dt; val dy = (targetY - pusher.position.y) / dt
            val scale = min(1f, config.pusherSpeed / max(.001f, hypot(dx, dy)))
            pusher.setLinearVelocity(dx * scale, dy * scale)
        }
        advanceEquipment(dt)
        if(config.physicalNeedles) advanceLocalMetal()
        world.step(dt, 6, 2)
        if(config.grabClumps && grab!=null) crowdHeldClump()
        if(session.rake && pusher.isActive && !config.grabClumps) crowdLocalContacts()
        // Process carried metal first so mouth entry cannot be lost when its clump releases.
        for (index in session.batch.items.indices) {
            val order=if(config.physicalNeedles) {
                if(index<config.needleCount) config.strawCount+index else index-config.needleCount
            } else index
            val item=session.batch.items[order]
            val body = bodies[item.id] ?: continue
            if(item.route != Route.INTAKE) {
                if(robotId==item.id) releaseRobot()
                if(item.material==Material.NEEDLE && config.physicalNeedles) detachMetal(item.id-config.strawCount)
                if(isHeld(item.id)) releaseGrip(item.id)
                world.destroyBody(body); bodies[item.id]=null; continue
            }
            // Curved material remains inside the tray except its contacted tip at the mouth.
            val c=cos(body.angle); val s=sin(body.angle)
            val ex = item.extent(c,s,true,config.pitReceiver)
            val ey = item.extent(c,s,false,config.pitReceiver)
            val cargo=config.physicalNeedles && item.material==Material.NEEDLE && carried[item.id-config.strawCount]!=null
            if(config.grabClumps && (isHeld(item.id) || cargo || powered[item.id])) session.manualPush(item.id)
            val contacted=if(config.grabClumps) isHeld(item.id) || cargo || powered[item.id] else pusher.isActive && item.manuallyPushed
            val sorting=config.physicalNeedles && item.material==Material.NEEDLE && item.id==grabbedId &&
                session.metalReceiverOwned && abs(body.position.x-config.metalDockX)<=config.metalDockHalfWidth
            val atMetalPort=if(config.pitReceiver) abs(body.position.y-FactoryConfig.METAL_Y)<=config.metalDockHalfWidth else body.position.y<=.03f
            if(sorting && atMetalPort && session.deliverMetal(item.id)) {
                item.x=body.position.x; item.y=body.position.y; item.angle=body.angle
                end(); world.destroyBody(body); bodies[item.id]=null; continue
            }
            val front=physicalFront(item,c,s)
            val crossed=if(config.pitReceiver) insidePit(body.position.x,body.position.y)
                else body.position.x+front >= session.intakeBoundary+.01f
            // A current local contact and a material tip beyond the real mouth are required.
            val loaded=(isHeld(item.id) || robotId==item.id) && carried.indices.any { carried[it]!=null && carrySources[it]==item.id }
            if(feeding && contacted && !cargo && crossed && (config.pitReceiver || !loaded ||
                body.position.x>=session.intakeBoundary-.1f) && session.gateFeed(item.id)) {
                // One physical cargo admission: never let its metal outrun the clump's gate.
                if(loaded) acceptLocalMetal(item.id)
                if(isHeld(item.id)) releaseGrip(item.id)
                if(robotId==item.id) releaseRobot()
                item.x=body.position.x; item.y=body.position.y; item.angle=body.angle
                world.destroyBody(body); bodies[item.id]=null; continue
            }
            // Negative-side overhang must not act as a phantom wall on the right.
            // Keep an idle tip at the lip; only current contact can push it across.
            val right=if(config.pitReceiver) config.width-ex else if(loaded) config.width+.02f else config.width-front+if(contacted) .02f else 0f
            val x = body.position.x.coerceIn(ex,right)
            // The visual envelope must stay inside the sloped guide cheeks too. The old
            // rectangle-only clamp could put a long needle back inside a guide collider.
            val guideMargin=if(session.guides && !config.pitReceiver) ((x+front-(config.width-2f))*.325f).coerceIn(0f,.65f) else 0f
            val y = body.position.y.coerceIn(if(sorting && !config.pitReceiver) -.85f else config.intakeMinY+ey+guideMargin, config.height-ey-guideMargin)
            if (x != body.position.x || y != body.position.y) body.setTransform(x, y, body.angle)
            val v = body.linearVelocity
            val scale = min(1f, 7f / max(.001f, v.len()))
            body.setLinearVelocity(v.x * scale, v.y * scale)
            body.angularVelocity = body.angularVelocity.coerceIn(-2.5f, 2.5f)
            item.x = x; item.y = y; item.angle = body.angle
            item.vx = body.linearVelocity.x; item.vy = body.linearVelocity.y; item.angularVelocity = body.angularVelocity
        }
    }
    /** Purchased power is explicit and local. With no equipment, this is a no-op. */
    private fun advanceEquipment(dt:Float) {
        robotReleaseAge=(robotReleaseAge+dt).coerceAtMost(10f)
        java.util.Arrays.fill(powered,false)
        if(installedEquipment!==session.equipment) {
            installedEquipment=session.equipment
            val selected=session.equipment.filter { it.kind==EquipmentKind.DIVIDER }
            for(b in dividers) world.destroyBody(b)
            dividers.clear();installedDividers=selected
            for(e in selected) {
                val body=world.createBody(BodyDef().apply { position.set(e.x,e.y);angle=e.degrees*PI.toFloat()/180 })
                val shape=PolygonShape().apply { setAsBox(e.dividerLength/2,.10f) }
                body.createFixture(FixtureDef().apply { this.shape=shape;friction=.25f;restitution=.15f;filter.categoryBits=8;filter.maskBits=3 })
                shape.dispose();dividers.add(body)
            }
        }
        if(session.phase!=BatchPhase.ACTIVE) { releaseRobot();return }
        for(e in session.equipment) if(e.kind==EquipmentKind.FAN && e.anchored) {
            val a=e.degrees*PI.toFloat()/180;val dx=cos(a);val dy=sin(a)
            for(item in session.batch.items) {
                val b=bodies[item.id] ?: continue
                if(item.route!=Route.INTAKE || isHeld(item.id) || item.id==robotId || !e.containsWind(b.position.x,b.position.y)) continue
                // Independent material tuning: buying strong airflow must not launch steel.
                val power=if(item.material==Material.NEEDLE) e.needlePower else e.power
                val falloff=(1-b.position.dst(e.x,e.y)/e.reach).coerceIn(0f,1f)
                val along=b.linearVelocity.x*dx+b.linearVelocity.y*dy
                // Limit pre-step acceleration too, not just post-step velocity, to avoid
                // tunnelling at high tiers. Native contacts and global 7m/s cap remain.
                val headroom=((7f-along)/dt).coerceAtLeast(0f)
                val force=minOf(power*falloff,420f,headroom)
                b.applyForceToCenter(b.mass*force*dx,b.mass*force*dy,true)
                powered[item.id]=true
            }
        }
        val robot=session.equipment.firstOrNull { it.kind==EquipmentKind.ROBOT }
        if(robot==null || !session.receivingEnabled) { releaseRobot();return }
        if(robotId>=0 && (bodies.getOrNull(robotId)==null || session.batch.items[robotId].route!=Route.INTAKE || isHeld(robotId))) releaseRobot()
        if(robotId<0) {
            if(robotReleaseAge<.18f) return
            var item:FactoryItem?=null;var closest=Float.MAX_VALUE
            for(candidate in session.batch.items) if(candidate.material==Material.STRAW && candidate.route==Route.INTAKE && !isHeld(candidate.id)) {
                val distance=hypot(candidate.x-robotX,candidate.y-robotY)
                if(distance<closest) {closest=distance;item=candidate}
            }
            if(item==null) return
            val b=bodies[item.id] ?: return
            val dx=b.position.x-robotX;val dy=b.position.y-robotY;val distance=hypot(dx,dy)
            val travel=dt*(1.6f+(robot.speed-1)*.18f).coerceAtMost(5f)
            if(distance>travel+.10f) { robotX+=dx/distance*travel;robotY+=dy/distance*travel;return }
            robotX=b.position.x;robotY=b.position.y;robotId=item.id;robotGripAge=0f
            for(f in b.fixtureList) {val filter=f.filterData;filter.categoryBits=2;filter.maskBits=8;f.filterData=filter}
            b.linearDamping=4f
            robotJoint=world.createJoint(MouseJointDef().apply {
                bodyA=ground;bodyB=b;target.set(b.position);maxForce=b.mass*config.grabAcceleration
                frequencyHz=7f;dampingRatio=.9f
            }) as MouseJoint
        }
        robotGripAge+=dt
        if(robotGripAge<.16f) {
            robotJoint?.target=robotTarget.set(robotX,robotY);powered[robotId]=true
            return
        }
        val dx=session.pitCenterX-robotX;val dy=FactoryConfig.PIT_Y-robotY;val distance=hypot(dx,dy)
        val travel=min(distance,dt*(1.6f+(robot.speed-1)*.18f).coerceAtMost(5f))
        if(distance>.001f) { robotX+=dx/distance*travel;robotY+=dy/distance*travel }
        robotJoint?.target=robotTarget.set(robotX,robotY);powered[robotId]=true
    }
    private fun releaseRobot() {
        if(robotId>=0) robotReleaseAge=0f
        robotJoint?.let { world.destroyJoint(it) };robotJoint=null
        for(i in carried.indices) if(carrySources[i]==robotId && robotId>=0) detachMetal(i)
        bodies.getOrNull(robotId)?.let {b->
            b.linearDamping=config.releasedDamping
            if(!isHeld(robotId)) for(f in b.fixtureList) {val filter=f.filterData;filter.categoryBits=1;filter.maskBits=10;f.filterData=filter}
        };robotId=-1
    }
    private fun acceptLocalMetal(source:Int) {
        for(i in carried.indices) if(carried[i]!=null && carrySources[i]==source) {
            val n=session.batch.needles[i]; val b=bodies[n.id] ?: continue
            // Heavy steel still behind the lip is left on the table, never teleported.
            val inside=if(config.pitReceiver) insidePit(b.position.x,b.position.y)
                else b.position.x+physicalFront(n,cos(b.angle),sin(b.angle))>=session.intakeBoundary+.01f
            if(inside && session.feed(n.id)) {
                n.x=b.position.x; n.y=b.position.y; n.angle=b.angle
                detachMetal(i); world.destroyBody(b); bodies[n.id]=null
            }
        }
    }
    private fun insidePit(x:Float,y:Float)=x>=session.pitLeft && x<=session.pitLeft+session.pitWidth &&
        abs(y-FactoryConfig.PIT_Y)<=FactoryConfig.PIT_HALF
    /** Only metal actually intersecting the selected stalks can be entrained. The bounded
     * contact springs are deliberately weaker than the hand grip; fast strokes can leave it. */
    private fun attachLocalMetal(held:Body,source:Int) {
        val clump=session.batch.items[source]
        val c=cos(held.angle); val s=sin(held.angle)
        for(i in carried.indices) {
            val n=session.batch.needles[i]; val body=bodies[n.id] ?: continue
            if(carried[i]!=null || n.route!=Route.INTAKE) continue
            var touching=false
            for(k in 0..6) {
                val a=(k/6f-.5f)*n.length
                val dx=n.x+cos(n.angle)*a-held.position.x; val dy=n.y+sin(n.angle)*a-held.position.y
                val x=c*dx+s*dy; val y=-s*dx+c*dy
                for(f in clump.strands) {
                    val u=(f.cosine*(x-f.x)+f.sine*(y-f.y))/f.length
                    val v=-f.sine*(x-f.x)+f.cosine*(y-f.y)
                    if(abs(u)<=.5f && abs(v-f.bend*(1-4*u*u))<=f.width/2+.025f) touching=true
                }
            }
            if(!touching) continue
            val dx=body.position.x-held.position.x; val dy=body.position.y-held.position.y
            carryOffsets[i].set(c*dx+s*dy,-s*dx+c*dy)
            carried[i]=world.createJoint(MouseJointDef().apply {
                bodyA=ground; bodyB=body; target.set(body.position)
                maxForce=body.mass*config.needleGrabAcceleration
                frequencyHz=config.needleGrabFrequency; dampingRatio=.9f
            }) as MouseJoint
            carrySources[i]=source
            body.linearDamping=4f
        }
    }
    private fun advanceLocalMetal() {
        for(item in session.batch.items) if(item.material==Material.STRAW && (isHeld(item.id) || robotId==item.id))
            bodies[item.id]?.let { attachLocalMetal(it,item.id) }
        for(i in carried.indices) {
            val joint=carried[i] ?: continue; val body=bodies[session.batch.needles[i].id] ?: continue
            val held=bodies.getOrNull(carrySources[i])
            if(held==null || !isHeld(carrySources[i]) && robotId!=carrySources[i]) { detachMetal(i); continue }
            val c=cos(held.angle); val s=sin(held.angle)
            val o=carryOffsets[i]
            carryTarget.set(held.position.x+c*o.x-s*o.y,held.position.y+s*o.x+c*o.y)
            if(body.position.dst(carryTarget)>.85f) detachMetal(i)
            else joint.target=carryTarget
        }
    }
    private fun detachMetal(i:Int) {
        carried[i]?.let { world.destroyJoint(it) }; carried[i]=null
        carrySources[i]=-1
        // Save loading can replace the manifest before this old adapter is retired.
        bodies.getOrNull(config.strawCount+i)?.linearDamping=config.releasedDamping
    }
    /** Leading physical material tip from simulation geometry. The cheap native proxy
     * represents a patch, not all its individual stalks; those stalks still have real ends. */
    private fun physicalFront(item:FactoryItem,c:Float,s:Float):Float {
        if(item.material==Material.NEEDLE) return abs(c)*item.length/2+abs(s)*.035f
        var front=Float.NEGATIVE_INFINITY
        for(f in item.strands) {
            val fc=c*f.cosine-s*f.sine; val fs=s*f.cosine+c*f.sine
            // Endpoints plus the exact quadratic extremum; no GPU/readback dependency.
            val denominator=8*fs*f.bend
            val extremum=if(abs(denominator)>.00001f) (-fc*f.length/denominator).coerceIn(-.5f,.5f) else 0f
            for(k in 0..2) {
                val u=if(k==2) extremum else k-.5f
                front=max(front,c*f.x-s*f.y+fc*u*f.length-fs*f.bend*(1-4*u*u)+abs(fs)*f.width/2)
            }
        }
        return front
    }
    /** Cheap soft patch collisions only among directly touched straw; never a global pull.
     * Native bodies still collide with the tool/guides. This bounded local crowding adds
     * lateral spreading without enabling thousands of overlapping rigid-body contacts. */
    private fun crowdLocalContacts() {
        var count=0
        for(item in session.batch.items) if(item.material==Material.STRAW && item.route==Route.INTAKE && item.manuallyPushed) {
            if(count==localContacts.size) break
            localContacts[count++]=item.id
        }
        val diameter=config.rakeCrowdingRadius*2
        for(i in 0 until count) for(j in i+1 until count) {
            val a=bodies[localContacts[i]] ?: continue; val b=bodies[localContacts[j]] ?: continue
            val dx=b.position.x-a.position.x; val dy=b.position.y-a.position.y
            val distance=hypot(dx,dy)
            if(distance>=diameter || distance<.001f) continue
            val strength=config.rakeCrowdingSpeed*(1-distance/diameter)
            val vx=dx/distance*strength; val vy=dy/distance*strength
            val av=a.linearVelocity; val ax=av.x; val ay=av.y
            val bv=b.linearVelocity; val bx=bv.x; val by=bv.y
            a.setLinearVelocity(ax-vx,ay-vy); b.setLinearVelocity(bx+vx,by+vy)
        }
    }
    private fun crowdHeldClump() {
        val held=bodies.getOrNull(grabbedId) ?: return
        for(item in session.batch.items) {
            if(isHeld(item.id) || item.material!=Material.STRAW || item.route!=Route.INTAKE) continue
            val other=bodies[item.id] ?: continue
            val dx=other.position.x-held.position.x; val dy=other.position.y-held.position.y
            val distance=hypot(dx,dy)
            if(distance<.001f || distance>=.48f) continue
            val speed=.30f*(1-distance/.48f)
            val v=other.linearVelocity
            other.setLinearVelocity(v.x+dx/distance*speed,v.y+dy/distance*speed)
        }
    }
    override fun close() { world.dispose() }
}
