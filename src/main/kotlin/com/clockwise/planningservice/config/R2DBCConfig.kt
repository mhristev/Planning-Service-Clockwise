package com.clockwise.planningservice.config

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.postgresql.codec.EnumCodec
import io.r2dbc.spi.ConnectionFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions
import org.springframework.data.r2dbc.dialect.PostgresDialect
import org.springframework.r2dbc.core.DatabaseClient
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Optional
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import java.time.Instant

@Configuration
class R2DBCConfig : AbstractR2dbcConfiguration() {

    @Value("\${spring.r2dbc.url}")
    private lateinit var url: String

    @Value("\${spring.r2dbc.username}")
    private lateinit var username: String

    @Value("\${spring.r2dbc.password}")
    private lateinit var password: String

    @Override
    override fun connectionFactory(): ConnectionFactory {
        val host = url.replace("r2dbc:postgresql://", "").split(":")[0]
        val port = url.split(":")[2].split("/")[0].toInt()
        val database = url.split("/")[1]

        return PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host(host)
                .port(port)
                .database(database)
                .username(username)
                .password(password)
                .build()
        )
    }

    @Bean
    override fun r2dbcCustomConversions(): R2dbcCustomConversions {
        return R2dbcCustomConversions.of(
            PostgresDialect.INSTANCE,
            listOf(
                TimestampToZonedDateTimeConverter(),
                ZonedDateTimeToTimestampConverter()
            )
        )
    }

    @ReadingConverter
    class TimestampToZonedDateTimeConverter : Converter<java.time.Instant, ZonedDateTime> {
        override fun convert(source: Instant): ZonedDateTime {
            return source.atZone(ZoneId.of("UTC"))
        }
    }

    @WritingConverter
    class ZonedDateTimeToTimestampConverter : Converter<ZonedDateTime, java.time.Instant> {
        override fun convert(source: ZonedDateTime): Instant {
            return source.toInstant()
        }
    }
} 