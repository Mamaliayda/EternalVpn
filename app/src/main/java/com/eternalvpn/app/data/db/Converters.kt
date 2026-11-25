package com.eternalvpn.app.data.db

import androidx.room.TypeConverter
import com.eternalvpn.app.data.model.Protocol
import com.eternalvpn.app.data.model.Security
import com.eternalvpn.app.data.model.Transport

class Converters {
    @TypeConverter fun fromProtocol(v: Protocol?): String? = v?.name
    @TypeConverter fun toProtocol(v: String?): Protocol? = v?.let { Protocol.valueOf(it) }

    @TypeConverter fun fromTransport(v: Transport?): String? = v?.name
    @TypeConverter fun toTransport(v: String?): Transport? = v?.let { Transport.valueOf(it) }

    @TypeConverter fun fromSecurity(v: Security?): String? = v?.name
    @TypeConverter fun toSecurity(v: String?): Security? = v?.let { Security.valueOf(it) }
}
