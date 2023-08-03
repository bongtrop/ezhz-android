package cc.ggez.ezhz.module.frida.model

enum class FridaItemState(s: Int) {
    NOT_INSTALL(0), INSTALLED(1), EXECUTING(2)
}
data class FridaItem(
    val tag: GithubTag,
    var state: FridaItemState = FridaItemState.NOT_INSTALL,
) {
    val isExecuted: Boolean
        get(): Boolean {
            return state == FridaItemState.EXECUTING
        }
    val isExecutable: Boolean
        get(): Boolean {
            return state == FridaItemState.INSTALLED || state == FridaItemState.EXECUTING
        }
    val isInstalled: Boolean
        get(): Boolean {
            return state == FridaItemState.EXECUTING || state == FridaItemState.INSTALLED
        }

    val isInstallable: Boolean
        get(): Boolean {
            return state != FridaItemState.EXECUTING
        }
}