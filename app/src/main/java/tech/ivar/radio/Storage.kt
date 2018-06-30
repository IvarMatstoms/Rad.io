package tech.ivar.radio

import java.io.File

interface StorageInterface {
    val name:String
    val id:String
    val radioButtonId:String
    val isDefault: Boolean
    fun isAvailable():Boolean
    fun getDir(): File

}

class StorageInternal: StorageInterface {
    override val isDefault: Boolean
        get() = true

    override val radioButtonId: String
        get() = "importOptionsFormStorageInternal"

    override fun isAvailable(): Boolean {
        return true
    }

    override val id: String
        get() = "internal"

    override fun getDir(): File {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val name: String
        get() = "Internal storage"

}

class StorageExternal: StorageInterface {
    override val isDefault: Boolean
        get() = false
    override val radioButtonId: String
        get() = "importOptionsFormStorageExternal"

    override fun isAvailable(): Boolean {
        return true
    }

    override val id: String
        get() = "external"

    override fun getDir(): File {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val name: String
        get() = "External storage"

}