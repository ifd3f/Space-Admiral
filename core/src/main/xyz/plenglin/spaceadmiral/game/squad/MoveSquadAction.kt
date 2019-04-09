package xyz.plenglin.spaceadmiral.game.squad

import com.badlogic.gdx.math.Vector2
import org.slf4j.LoggerFactory
import xyz.plenglin.spaceadmiral.game.ship.Ship
import xyz.plenglin.spaceadmiral.game.ship.ShipAction
import xyz.plenglin.spaceadmiral.net.game.io.s2c.update.ActionDTO
import xyz.plenglin.spaceadmiral.util.State
import xyz.plenglin.spaceadmiral.util.StateScheduler
import java.util.*

class MoveSquadAction(squad: Squad, val target: SquadTransform) : SquadAction(squad) {
    private val transforms = target.generateChildTransforms()
    private val shipsEnRoute = HashSet<Ship>()
    override val expectedEndPos: Vector2 get() = target.transform.posGlobal

    override fun initialize(parent: StateScheduler) {
        logger.info("Initializing move action {}", this)
        squad.ships.forEach {
            it.stateScheduler.nextState = MoveShipAction(it)
        }
        shipsEnRoute.clear()
        shipsEnRoute.addAll(squad.ships)
        squad.transform.set(target)
    }

    override fun update() {

    }

    override fun shouldTerminate(): Boolean {
        logger.trace("ships still moving over: {}", shipsEnRoute)
        return shipsEnRoute.isEmpty()
    }

    override fun terminate(): State? {
        logger.info("Terminating move action {}", this)
        return null
    }

    private fun onShipFinished(ship: Ship) {
        logger.trace("Ship finished moving to destination: {}", ship)
        if (!shipsEnRoute.remove(ship)) {
            logger.error("Ship {} was not managed by this {}!", ship, this)
        }
    }

    private val speed = squad.template.speed
    private val epsilon2 = squad.template.speed * squad.template.speed

    inner class MoveShipAction(ship: Ship) : ShipAction(this@MoveSquadAction, ship) {
        private var error = Vector2(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        private val target = transforms[ship.transformIndex]

        override fun initialize(parent: StateScheduler) {
            ship.transform.angleLocal = target.posGlobal.angle(ship.transform.posGlobal)
        }

        override fun update() {
            error = target.posGlobal.cpy().sub(ship.transform.posGlobal)
            val delta = error.cpy().setLength(speed)
            ship.velocity.set(delta)
            logger.trace("Moving {} at {} to {} (error={})", ship, ship.transform.posGlobal, target.posGlobal, error.len())
        }

        override fun shouldTerminate(): Boolean {
            return error.len2() < epsilon2
        }

        override fun interrupt() {
            logger.trace("Interrupting ship move for {}", this)
            onShipFinished(ship)
        }

        override fun terminate(): State? {
            logger.debug("Terminating ship move for {}", this)
            onShipFinished(ship)
            ship.velocity.set(0f, 0f)
            ship.transform.setLocalPosition(target.posGlobal)
            ship.transform.angleLocal = target.angleGlobal
            return null
        }

    }

    override fun toString(): String {
        return "SquadAction($squad, $target)"
    }

    override fun toDTO(): ActionDTO {
        return DTO(target)
    }

    data class DTO(override val endPos: SquadTransform) : ActionDTO

    private companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(MoveSquadAction::class.java)
    }

}