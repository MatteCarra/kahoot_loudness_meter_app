package mattecarra.loudnessmeter

import android.os.Handler
import mattecarra.loudnessmeter.MainActivity.Companion.DATA_PACKET
import mattecarra.loudnessmeter.MainActivity.Companion.DISCONNECT_PACKET
import mattecarra.loudnessmeter.packet.DataPacket
import mattecarra.loudnessmeter.packet.DisconnectPacket
import mattecarra.loudnessmeter.packet.KeepAlive
import mattecarra.loudnessmeter.packet.Packet
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

class LoudnessDataSocket(val ip: String, val handler: Handler): Runnable {
    private val packetProtocol = PacketProtocol()
    private var socket: Socket? = null
    private var reader: DataInputStream? = null
    private var writer: DataOutputStream? = null

    init {
        packetProtocol.registerIncoming(0, DisconnectPacket::class.java)
        packetProtocol.registerIncoming(1, KeepAlive::class.java)
        packetProtocol.registerIncoming(2, DataPacket::class.java)

        packetProtocol.registerOutgoing(0, DisconnectPacket::class.java)
        packetProtocol.registerOutgoing(1, KeepAlive::class.java)
    }

    private fun handlePacket(packet: Packet) {
        when(packet) {
            is DisconnectPacket -> {
                socket?.close()
                socket = null

                handler.obtainMessage(DISCONNECT_PACKET).sendToTarget()
            }
            is DataPacket -> {
                handler.obtainMessage(DATA_PACKET, packet.data).sendToTarget()
            }
            is KeepAlive -> {
                sendPacket(KeepAlive(packet.id!!))
            }
        }
    }

    fun sendPacket(packet: Packet) {
        writer?.let {
            val id = packetProtocol.getOutgoingId(packet::class.java)

            it.writeByte(id)
            packet.write(DataOutputStream(it))
        }
    }

    fun connect(): LoudnessDataSocket {
        Thread(this).start()
        return this
    }

    override fun run() {
        try {
            socket = Socket(ip, 3333)
            reader = DataInputStream(socket!!.getInputStream())
            writer = DataOutputStream(socket!!.getOutputStream())

            while (socket?.isConnected == true) {
                val id = reader!!.read()
                val packet = packetProtocol.createIncomingPacket(id)
                packet.read(reader!!)

                handlePacket(packet)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()

            handler.obtainMessage(DISCONNECT_PACKET).sendToTarget()
        } finally {
            if(socket?.isConnected == true)
                socket?.close()
        }
    }
}