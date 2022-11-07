package com.github.ljufa.sma.tw.server.db

import com.github.ljufa.sma.tw.server.config
import org.lmdbjava.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.ByteBuffer

object DataSource {
    val log: Logger = LoggerFactory.getLogger(DataSource::class.java)
    private var dbPurged = false
    private val env = prepareDbEnv()
    val reactions: Dbi<ByteBuffer> = env.openDbi("reactions", DbiFlags.MDB_CREATE)
    val dateIndex: Dbi<ByteBuffer> = env.openDbi("dateIndex", DbiFlags.MDB_CREATE)
    val possiblySensitiveIndex: Dbi<ByteBuffer> = env.openDbi("possiblySensitiveIndex", DbiFlags.MDB_CREATE)
    val langIndex: Dbi<ByteBuffer> = env.openDbi("twidLangIndex", DbiFlags.MDB_CREATE, DbiFlags.MDB_DUPSORT)
    val matchedRulesIndex: Dbi<ByteBuffer> = env.openDbi("matchedRules", DbiFlags.MDB_CREATE, DbiFlags.MDB_DUPSORT)
    val hashTags: Dbi<ByteBuffer> = env.openDbi("hashTags", DbiFlags.MDB_CREATE, DbiFlags.MDB_DUPSORT)
    val userMentions: Dbi<ByteBuffer> = env.openDbi("userMentions", DbiFlags.MDB_CREATE, DbiFlags.MDB_DUPSORT)
    val urls: Dbi<ByteBuffer> = env.openDbi("refUrls", DbiFlags.MDB_CREATE, DbiFlags.MDB_DUPSORT)

    fun getEnv(): Env<ByteBuffer> {
        return env
    }

    fun readTxn(): Txn<ByteBuffer> {
        return env.txnRead()
    }

    fun writeTxn(): Txn<ByteBuffer> {
        return env.txnWrite()
    }

    fun getDbPurged(): Boolean {
        return dbPurged
    }


    private fun prepareDbEnv(): Env<ByteBuffer> {
        try {
            File(config.database.rootDirPath).mkdir()
            if (config.database.purgeOnBootToken != null) {
                dbPurged =
                    deleteExistingData(config.database.purgeOnBootToken)
            }
            val path = File(config.database.rootDirPath)
            if(!path.exists()){
                path.createNewFile()
            }
            val env: Env<ByteBuffer> =
                Env.create()
                    // LMDB also needs to know how large our DB might be. Over-estimating is OK.
                    .setMapSize(1048576000)
                    // LMDB also needs to know how many DBs (Dbi) we want to store in this Env.
                    .setMaxDbs(10)
                    .open(path, EnvFlags.MDB_NOLOCK, EnvFlags.MDB_NOSYNC)
            return env
        } catch (ex: Exception) {
            ex.printStackTrace()
            throw ex
        }
    }

    private fun deleteExistingData(purgeDbToken: String): Boolean {
        val tokenFile = File("${config.database.rootDirPath}/token.deletetoken")
        if (tokenFile.exists()) {
            if (purgeDbToken == tokenFile.readText().trim()) {
                return false
            }
        } else {
            tokenFile.createNewFile()
        }
        log.info("Deleting existing db with token {}", purgeDbToken)
        File(config.database.rootDirPath).listFiles()
            ?.filter { !it.name.endsWith(".deletetoken") }
            ?.forEach { file ->
                log.info("Deleting old file {}", file.absolutePath)
                file.deleteRecursively()
            }
        tokenFile.writeText(purgeDbToken)
        return true
    }

}