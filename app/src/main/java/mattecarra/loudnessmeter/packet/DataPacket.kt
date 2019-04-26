package mattecarra.loudnessmeter.packet

import java.io.DataInputStream
import java.io.DataOutputStream

class DataPacket: Packet {
    var data: Int? //unsigned short
        private set

    constructor() {
        data = null
    }

    constructor(id: Int) {
        this.data = id
    }

    override fun write(output: DataOutputStream) {
        output.writeShort(data!!)
    }

    override fun read(input: DataInputStream) {
        data = input.readUnsignedShort()
    }
}