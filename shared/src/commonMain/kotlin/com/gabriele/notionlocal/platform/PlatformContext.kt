package com.gabriele.notionlocal.platform

/**
 * **Da dove si parte per trovare file e impostazioni** (dal 04/11/2026, verso
 * Windows e Mac).
 *
 * Sul telefono è proprio il `Context` di Android (`androidMain`): chi passa un
 * Context, come ha sempre fatto, non cambia niente. Sul PC (`desktopMain`) è
 * la cartella dei dati di PageFlow.
 */
expect abstract class PlatformContext
