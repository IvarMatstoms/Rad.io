package tech.ivar.radio

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

interface StorageInterface {
    val name: String
    val id: String
    val radioButtonId: String
    val isDefault: Boolean
    fun isAvailable(): Boolean
    fun getStationsDir(context: Context): File
    fun getIndexFile(context: Context): File

}

class StorageInternal : StorageInterface {
    override val isDefault: Boolean
        get() = true

    override val radioButtonId: String
        get() = "importOptionsFormStorageInternal"

    override fun isAvailable(): Boolean {
        return true
    }

    override val id: String
        get() = "internal"

    override fun getStationsDir(context: Context): File {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        val file=File(context.filesDir, "stations")
        file.mkdirs()
        return file
    }

    override fun getIndexFile(context: Context): File {
        return File(context.filesDir, "index.json")
    }

    override val name: String
        get() = "Internal storage"

}

class StorageExternal : StorageInterface {
    override val isDefault: Boolean
        get() = false
    override val radioButtonId: String
        get() = "importOptionsFormStorageExternal"

    override fun isAvailable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

    }

    override val id: String
        get() = "external"

    override fun getStationsDir(context: Context): File {
        val file = File(context.getExternalFilesDir(
                Environment.DIRECTORY_DOCUMENTS), "rad.io")
        //val file=File(Environment.getExternalStorageDirectory(), "rad.io")
        file.mkdirs()
        val stationsFile = File(file,"stations")
        stationsFile.mkdirs()

        //Log.w("D",indexFile.toURI().toString())
        return stationsFile
    }

    override fun getIndexFile(context: Context): File {
        val file = File(context.getExternalFilesDir(
                Environment.DIRECTORY_DOCUMENTS), "rad.io")
        //val file=File(Environment.getExternalStorageDirectory(), "rad.io")
        file.mkdirs()
        val indexFile = File(file,"index.json")
        //Log.w("D",indexFile.toURI().toString())
        return indexFile
    }

    override val name: String
        get() = "External storage"

}