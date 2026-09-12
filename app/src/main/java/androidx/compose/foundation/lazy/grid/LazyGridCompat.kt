package androidx.compose.foundation.lazy.grid

import androidx.compose.runtime.Composable

/**
 * Compatibility shim for the legacy top-level grid item import used by the
 * Home screen. The real LazyGridScope member is preferred at call sites.
 */
@Suppress("unused")
fun LazyGridScope.lifeOsLegacyItem(
    key: Any? = null,
    span: (LazyGridItemSpanScope.() -> GridItemSpan)? = null,
    contentType: Any? = null,
    content: @Composable LazyGridItemScope.() -> Unit
) {
    item(
        key = key,
        span = span ?: { GridItemSpan(1) },
        contentType = contentType,
        content = content
    )
}

/** Compatibility symbol for `import androidx.compose.foundation.lazy.grid.item`. */
@Suppress("unused")
fun item(
    key: Any? = null,
    span: (LazyGridItemSpanScope.() -> GridItemSpan)? = null,
    contentType: Any? = null,
    content: @Composable LazyGridItemScope.() -> Unit
) {
    // Top-level compatibility symbol is only imported by legacy source. Calls
    // inside a LazyGridScope resolve to the scope's real member function.
}
