fun String.popFirstWord(): String = dropWhile { it != ' ' }.dropWhile { it == ' ' }