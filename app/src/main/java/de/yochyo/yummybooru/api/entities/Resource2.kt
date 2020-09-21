package de.yochyo.yummybooru.api.entities

import java.io.InputStream

open class Resource2(override val mimetype: String, open val input: InputStream) : IResource