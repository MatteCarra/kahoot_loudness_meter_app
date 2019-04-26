package mattecarra.loudnessmeter

import android.util.SparseArray
import androidx.core.util.contains
import mattecarra.loudnessmeter.packet.Packet
import java.util.HashMap

class PacketProtocol {
    private val incoming = SparseArray<Class<out Packet>>()
    private val outgoing = HashMap<Class<out Packet>, Int>()

    fun registerOutgoing(id: Int, packet: Class<out Packet>) {
        this.outgoing[packet] = id
    }

    fun registerIncoming(id: Int, packet: Class<out Packet>) {
        this.incoming.put(id, packet)
    }

    fun createIncomingPacket(id: Int): Packet {
        val packet: Class<out Packet>
        if (id < 0 || !this.incoming.contains(id) || this.incoming.get(id) == null) {
            throw IllegalArgumentException("Invalid packet id: $id")
        } else {
            packet = this.incoming.get(id)
        }

        try {
            val constructor = packet.getDeclaredConstructor()
            if (!constructor.isAccessible) {
                constructor.isAccessible = true
            }

            return constructor.newInstance()
        } catch (e: NoSuchMethodError) {
            throw IllegalStateException("Packet \"" + id + ", " + packet.name + "\" does not have a no-params constructor for instantiation.")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to instantiate packet \"" + id + ", " + packet.name + "\".", e)
        }
    }

    fun getOutgoingId(packet: Class<out Packet>): Int {
        val res: Int?
        if (this.outgoing.containsKey(packet)) {
            res = this.outgoing[packet]
            if (res != null) {
                return res
            }
        }

        throw IllegalArgumentException("Unregistered outgoing packet class: " + packet.name)
    }
}