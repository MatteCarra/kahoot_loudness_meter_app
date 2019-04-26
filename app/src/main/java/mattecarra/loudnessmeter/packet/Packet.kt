package mattecarra.loudnessmeter.packet

import java.io.DataInputStream
import java.io.DataOutputStream

interface Packet {
    fun read(input: DataInputStream)

    fun write(output: DataOutputStream)
}