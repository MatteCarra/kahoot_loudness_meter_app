package mattecarra.loudnessmeter.packet

import java.io.DataInputStream
import java.io.DataOutputStream

class KeepAlive: Packet {
    var id: Int?
        private set

    constructor() {
        id = null
    }

    constructor(id: Int) {
        this.id = id
    }

    override fun write(input: DataOutputStream) {
        input.writeShort(id!!)
    }

    override fun read(input: DataInputStream) {
        id = input.readUnsignedShort()
    }
}