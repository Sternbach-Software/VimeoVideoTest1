package contributors

fun main(args: Array<String>) {
        setDefaultFontSize(18f)
        ContributorsUI().apply {
            pack()
            setLocationRelativeTo(null)
            isVisible = true
        }
    }

