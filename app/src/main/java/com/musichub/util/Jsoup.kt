package com.musichub.util

import org.jsoup.nodes.Element
import org.jsoup.parser.Parser


private fun Element.getElementOrNull(elementPredicate: (Element) -> Boolean): Element? {
    if (elementPredicate(this))
        return this
    for (child in this.children()) {
        val elm = child.getElementOrNull(elementPredicate)
        if (elm != null)
            return elm
    }
    return null
}

internal fun Element.getElementByClass(className: String): Element {
    return this.getElementOrNull { className in it.classNames() } ?: throw IllegalArgumentException(
        "no Element containing className '$className'"
    )
}

internal fun Element.getElementByClassOrNull(className: String): Element? {
    return this.getElementOrNull { className in it.classNames() }
}

internal fun Element.getElementByTag(tagName: String): Element {
    return this.getElementOrNull { it.tagName() == tagName }
        ?: throw IllegalArgumentException("no Element with tagName '$tagName'")
}

internal fun unescapeHtml(htmlString: String): String {
    return Parser.unescapeEntities(htmlString, true)
}