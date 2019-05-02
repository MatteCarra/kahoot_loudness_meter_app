package mattecarra.loudnessmeter.protocol

import android.os.Handler
import mattecarra.loudnessmeter.MainActivity.Companion.DATA_PACKET
import mattecarra.loudnessmeter.MainActivity.Companion.DISCONNECT_PACKET
import mattecarra.loudnessmeter.packet.*
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentLinkedQueue

class LoudnessClient(ip: String, private val handler: Handler) {
    private val packetProtocol = PacketProtocol()
    private var connectionThread: Thread
    private var keepAliveThread: Thread
    private var address: InetAddress = InetAddress.getByName(ip)
    var connected = false
        private set

    private var lastPacketReceived: Long = -1

    private val buf = ByteArray(Companion.BUFF_LEN)
    private val reader = DataInputStream(ByteArrayInputStream(buf))
    private val writer = ByteArrayOutputStream(buf)
    private val outPackets = ConcurrentLinkedQueue<Packet>()

    private var socket: DatagramSocket? = null

    init {
        packetProtocol.registerIncoming(0, DataPacket::class.java)
        packetProtocol.registerIncoming(2, DisconnectPacket::class.java)

        packetProtocol.registerOutgoing(0, ConnectPacket::class.java)
        packetProtocol.registerOutgoing(1, KeepAlive::class.java)
        packetProtocol.registerOutgoing(2, DisconnectPacket::class.java)

        connectionThread = Thread {
            socket = DatagramSocket()
            sendPacket(ConnectPacket())

            try {
                while (connected) {
                    sendPackets()
                    receiveAndElaboratePacket()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()

                handler.obtainMessage(DISCONNECT_PACKET).sendToTarget()
            } finally {
                socket?.close()
            }
        }

        keepAliveThread = Thread {
            try {
                lastPacketReceived = System.currentTimeMillis()

                while (connected) {
                    sendPacket(KeepAlive())

                    if(System.currentTimeMillis() - lastPacketReceived > 5000) { //if server times out force reconnect
                        socket?.close()

                        socket = DatagramSocket()
                        sendPacket(ConnectPacket())
                    }

                    Thread.sleep(1000)
                }
            } catch (ignored: InterruptedException) {

            }
        }
    }

    fun connect() {
        if(!connected) {
            connected = true
            connectionThread.start()
            keepAliveThread.start()
        }
    }

    fun stop() {
        connected = false
        connectionThread.interrupt()
        keepAliveThread.interrupt()
    }

    private fun receiveAndElaboratePacket() {
        reader.reset()

        val datagramPacket = DatagramPacket(buf, buf.size, address, Companion.PORT)
        socket?.receive(datagramPacket)

        if(datagramPacket.length > 0) {
            val id = reader.read()
            val packet = packetProtocol.createIncomingPacket(id)
            packet.read(reader)

            lastPacketReceived = System.currentTimeMillis()

            handlePacket(packet)
        } else {
            Thread.sleep(50)
        }
    }

    private fun handlePacket(packet: Packet) {
        when(packet) {
            is DisconnectPacket -> {
                stop()
            }
            is DataPacket -> {
                handler.obtainMessage(DATA_PACKET, packet.data).sendToTarget()
            }
        }
    }

    private fun sendPackets() {
        var pack = outPackets.poll()

        while(pack != null) {
            writer.reset()

            val id = packetProtocol.getOutgoingId(pack::class.java)
            writer.write(id)
            pack.write(DataOutputStream(writer))

            socket?.send(DatagramPacket(writer.toByteArray(), writer.size(), address, Companion.PORT))

            pack = outPackets.poll()
        }
    }

    private fun sendPacket(packet: Packet) {
        outPackets.add(packet)
    }

    companion object {
        private const val PORT = 3333
        private const val BUFF_LEN = 256
    }
}