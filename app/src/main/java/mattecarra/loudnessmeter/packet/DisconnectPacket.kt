package mattecarra.loudnessmeter.packet

import java.io.DataInputStream
import java.io.DataOutputStream

class DisconnectPacket: Packet {
    override fun write(input: DataOutputStream) {}

    override fun read(input: DataInputStream) {}

}