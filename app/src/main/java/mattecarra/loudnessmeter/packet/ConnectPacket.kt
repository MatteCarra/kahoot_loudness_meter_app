package mattecarra.loudnessmeter.packet

import java.io.DataInputStream
import java.io.DataOutputStream

class ConnectPacket: Packet {
    override fun write(output: DataOutputStream) {}

    override fun read(input: DataInputStream) {}
}