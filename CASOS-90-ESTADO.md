# Seguimiento individual de 90 casos

Base: run 43693645479, 452 pruebas/90 fallos según inventario del propietario. El último HEAD 86e85cf sigue sin conclusión al preparar este lote. Ningún caso se marca resuelto sin nueva ejecución; los cambios de helper son candidatos, no evidencia de éxito.

| # | Caso | Estado de verificación | Cambio / siguiente causa |
| --- | --- | --- | --- |
| 1 | `AlphabetIndicatorTest.indicatorKeepsAVisibleGapFromActualRailInkDuringHorizontalPulls` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 2 | `AlphabetIndicatorTest.indicatorStaysCenteredAndSeparatedAcrossLetterBoundariesAndTheRailEnd` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 3 | `AlphabetIndicatorTest.indicatorCentersTheActualInkOfNarrowWideAndNonAlphabeticGlyphs` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 4 | `AlphabetIndicatorTest.favoriteCellShowsARealStarBubbleAndKeepsItWhileScrubbingBackFromLetters` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 5 | `AlphabetIndicatorTest.letterAndFavoriteIndicatorsFollowLiveAccentAndDarkModeChanges` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 6 | `AlphabetSettingsTest.resetColorAndFreeMovementDoNotAlterAppListAppearance` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 7 | `AlphabetSettingsTest.fourthPersonalizationEntryOpensTheRightSidePreview` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 8 | `AppNameVisibilitySettingsTest.inlineButtonsSupportAllNameCombinationsAndTheIndependentFolderSwitch` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 9 | `AppNotificationUiTest.homePreviewArrowOpensNotificationAboveShortcutsAndLeftSwipeClearsOnlyIt` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 10 | `ClockCalendarRenderingTest.weekTimeAndWeekdayHaveIndependentVisibilityAndClickTargets` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 11 | `ClockGlyphColorTest.recoloringChangesOnlyGlyphFillAndPreservesSeparatedInkCoverage` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 12 | `ClockRegionRenderingTest.featureGatesHideOnlyTheirOwnElementsAndCollapseEmptyRegions` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 13 | `ClockRegionRenderingTest.largeTextPreservesBothPhysicalEdgesInRtl` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 14 | `ClockRegionRenderingTest.rowTypographyDoesNotInheritPrimaryClockSpacingOrShadow` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 15 | `ClockRegionRenderingTest.primaryRotationSwapsItsBoundsAndPreservesVerticalRegionsAndTouchTargets` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 16 | `ClockRegionRenderingTest.regionOrderAndAlignmentPreserveIndependentClickTargets` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 17 | `ClockRegionRenderingTest.rotatingTallRegionsFitsAvailableWidthWithoutDroppingRows` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 18 | `ClockRegionRenderingTest.rotatingAllRegionsSwapsTheBlockAndKeepsAgendaBelowAndUnrotated` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 19 | `ClockSettingsTest.searchIncludesOriginalNamesAndDefaultStaysAvailable` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 20 | `ClockStyleEditorModesTest.editingASavedFontColorAndReturningKeepsTheClockDraftAndRelativeOffsets` | Corrección candidata; pendiente CI | Navegación compartida de pestaña/grupo/lista (ClockEditorNavigation.kt) |
| 21 | `ClockStyleEditorModesTest.pointerDragsMoveIconsAndDividerWithoutTogglingSelection` | Corrección candidata; pendiente CI | Navegación compartida de pestaña/grupo/lista (ClockEditorNavigation.kt) |
| 22 | `ClockStyleEditorModesTest.systemBackAlsoGuardsChangesAndDiscardDoesNotSave` | Corrección candidata; pendiente CI | Navegación compartida de pestaña/grupo/lista (ClockEditorNavigation.kt) |
| 23 | `ClockStyleEditorModesTest.applyingAPresetCopiesItsTypographyAndRestoresVisiblePartsInRegions` | Corrección candidata; pendiente CI | Navegación compartida de pestaña/grupo/lista (ClockEditorNavigation.kt) |
| 24 | `ClockStyleEditorModesTest.revertingTheOnlyEditDoesNotPromptForUnsavedChanges` | Corrección candidata; pendiente CI | Navegación compartida de pestaña/grupo/lista (ClockEditorNavigation.kt) |
| 25 | `ClockStyleEditorModesTest.partColorsRemainSharedWhileSwitchingBetweenPresetAndStyleTabs` | Corrección candidata; pendiente CI | Navegación compartida de pestaña/grupo/lista (ClockEditorNavigation.kt) |
| 26 | `ClockStyleEditorModesTest.miscReordersRegionsWithoutChangingTheirContent` | Corrección candidata; pendiente CI | Navegación compartida de pestaña/grupo/lista (ClockEditorNavigation.kt) |
| 27 | `ClockStyleEditorModesTest.rotationSwitchesRetainPrimaryWhileAllIsOnAndCanBothBeOff` | Corrección candidata; pendiente CI | Navegación compartida de pestaña/grupo/lista (ClockEditorNavigation.kt) |
| 28 | `ClockStyleEditorModesTest.clearingPrimaryHidesItsControlsAndLeavesTheOtherRegionsSelected` | Corrección candidata; pendiente CI | Navegación compartida de pestaña/grupo/lista (ClockEditorNavigation.kt) |
| 29 | `ClockStyleEditorModesTest.appBarBackOffersCancelDiscardOrSaveWithoutLosingTheDraft` | Corrección candidata; pendiente CI | Navegación compartida de pestaña/grupo/lista (ClockEditorNavigation.kt) |
| 30 | `ClockStyleEditorModesTest.failedSaveFromExitDialogKeepsDraftAndCanBeRetried` | Corrección candidata; pendiente CI | Navegación compartida de pestaña/grupo/lista (ClockEditorNavigation.kt) |
| 31 | `ClockStyleEditorModesTest.clockAndWeatherOptionsFollowTheRegionThatOwnsTheElement` | Corrección candidata; pendiente CI | Navegación compartida de pestaña/grupo/lista (ClockEditorNavigation.kt) |
| 32 | `ClockStylePersistenceTest.invalidRegionEntriesAreNormalizedWithoutDiscardingValidSettings` | Corrección candidata; pendiente CI | Normalización de Agenda/Task antes de comparar persistencia (ClockStylePersistenceTest.kt) |
| 33 | `ClockStylePersistenceTest.emptyPrimaryRegionOrderAndIndependentTypographyRoundTrip` | Corrección candidata; pendiente CI | Normalización de Agenda/Task antes de comparar persistencia (ClockStylePersistenceTest.kt) |
| 34 | `ClockStyleSettingsTest.importedFontsCanBeSelectedAndReplacedWithTheDefault` | Corrección candidata; pendiente CI | Navegación compartida y montaje de fila de Ajustes (ClockEditorNavigation.kt) |
| 35 | `ClockStyleSettingsTest.previewsAreLiveAndPrimaryElementsKeepIndependentValuesUntilApply` | Corrección candidata; pendiente CI | Navegación compartida y montaje de fila de Ajustes (ClockEditorNavigation.kt) |
| 36 | `ClockStyleSettingsTest.failedApplyKeepsTheEditorAndCanBeRetried` | Corrección candidata; pendiente CI | Navegación compartida y montaje de fila de Ajustes (ClockEditorNavigation.kt) |
| 37 | `ClockStyleSettingsTest.resetButtonsResetOnlyTheirOwnSliderAndBackDiscardsTheDraft` | Corrección candidata; pendiente CI | Navegación compartida y montaje de fila de Ajustes (ClockEditorNavigation.kt) |
| 38 | `ClockStyleWallpaperTest.standaloneEditorDisplaysTheSystemWallpaperInsideItsPreviewOnly` | Corrección candidata; pendiente CI | Montar fila del editor desde lista lazy (ClockStyleWallpaperTest.kt) |
| 39 | `DrawerAnchoringTest.overflowingGroupKeepsItsAnchorAndRemainsPartOfTheCompleteScrollableList` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 40 | `DrawerAnchoringTest.shortTailClampsNaturallyAndReleaseDoesNotJumpToAnArtificialAnchor` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 41 | `DrawerAnchoringTest.catalogueThatAlreadyFitsNeverMovesItsSelectedGroupOnHoldOrRelease` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 42 | `FavoritesEditorTest.selectedAppsComeFirstAndAllAppsKeepTheirChecksAndAlphabeticOrder` | Corrección candidata; pendiente CI | Montar filas de apps desde category_editor_list (FavoritesEditorTest.kt) |
| 43 | `FavoritesEditorTest.handlesDragBothDirectionsWithoutTogglingAndOrderIsUsedOnHome` | Corrección candidata; pendiente CI | Montar filas de apps desde category_editor_list (FavoritesEditorTest.kt) |
| 44 | `FavoritesEditorTest.swappingRowsKeepsTheDraggedRowUnderTheFingerAndNeighborsMoveContinuously` | Corrección candidata; pendiente CI | Montar filas de apps desde category_editor_list (FavoritesEditorTest.kt) |
| 45 | `FavoritesEditorTest.ordinaryListScrollingNeverChangesFavoritesAndAnEmptySelectionCanBeRefilled` | Corrección candidata; pendiente CI | Montar filas de apps desde category_editor_list (FavoritesEditorTest.kt) |
| 46 | `FavoritesEditorTest.selectionsAppendToTheSelectedSectionAndUncheckingDoesNotRemoveTheAppFromAllApps` | Corrección candidata; pendiente CI | Montar filas de apps desde category_editor_list (FavoritesEditorTest.kt) |
| 47 | `HomeAgendaHeaderTest.disablingCalendarRemovesTheSecondRowAndItsSpaceWithoutHidingWeather` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 48 | `HomeAgendaHeaderTest.dateAndAgendaMatchAppLabelSizeAndClockUsesMediumWeight` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 49 | `HomeMediaPlayerTest.permissionNeedsAnExplicitActionAndToggleNeverGrantsItSilently` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 50 | `HomeMediaPlayerTest.rowFitsBetweenDateAndFavoritesWithAlignedCoverAndRealActions` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 51 | `HomeMediaPlayerTest.noPermissionDisabledPreferenceAndSettingsLoadingLeaveHomeClean` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 52 | `HomepageCategoriesTest.homepageHasGroupsInsteadOfDirectAppsAndOnlyCheckboxesRemoveItems` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 53 | `IconDesignerTest.shortcutPolicyIsAnAllOnlyDraftAboveEffects` | Corrección candidata; pendiente CI | Montar controles antes de scroll/acciones (IconDesignerTest.kt) |
| 54 | `IconDesignerTest.singleShowsInheritedDisplayAndRestoresSavedOverride` | Corrección candidata; pendiente CI | Montar controles antes de scroll/acciones (IconDesignerTest.kt) |
| 55 | `IconDesignerTest.sourcePickerAndColorResetKeepTheDraftUntilSave` | Corrección candidata; pendiente CI | Montar controles antes de scroll/acciones (IconDesignerTest.kt) |
| 56 | `IconDesignerTest.shapeSlidersOnlyAppearForTheirShapeAndSaveIndependentValues` | Corrección candidata; pendiente CI | Montar controles antes de scroll/acciones (IconDesignerTest.kt) |
| 57 | `IconDesignerTest.displayModeIsFirstAndOnlyItsOwnControlsRemain` | Corrección candidata; pendiente CI | Montar controles antes de scroll/acciones (IconDesignerTest.kt) |
| 58 | `IconDesignerTest.singleControlsStayDraftsUntilSaveAndColorPickerStaysBelowPreview` | Corrección candidata; pendiente CI | Montar controles antes de scroll/acciones (IconDesignerTest.kt) |
| 59 | `IconDesignerTest.allIsTheDefaultAndSingleReusesAppSelection` | Corrección candidata; pendiente CI | Montar controles antes de scroll/acciones (IconDesignerTest.kt) |
| 60 | `IconPackUiTest.themeOpensFullScreenAndMultiplePacksCanBeReorderedAndRemoved` | Corrección candidata; pendiente CI | Montar fila de paquetes desde lista lazy (IconPackUiTest.kt) |
| 61 | `LauncherInteractionTest.homeVerticalGesturesNeverOpenDrawerAndFloatingButtonLongPressOpensSettings` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 62 | `LauncherInteractionTest.floatingButtonRippleStaysInsideItsCircularSurface` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 63 | `LauncherInteractionTest.longPressOpensDetailsAndFavoritesAreEditedOnlyInThePanel` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 64 | `LauncherInteractionTest.fullListsCanScrollToTheirLastAppWithoutOpeningShortcuts` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 65 | `LauncherInteractionTest.rightSwipeOpensProvidedShortcutRowsAndDoesNotLaunchTheApp` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 66 | `LauncherInteractionTest.tappingDateShowsAgendaWithEventsAndNewEventAction` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 67 | `LauncherReloadTest.missingMismatchedOrDamagedPreviewFallsBackAndCanBeReplaced` | Corrección candidata; pendiente CI | Preservar applicationContext aislado de fixture (LauncherReloadTest.kt) |
| 68 | `LauncherReloadTest.returningHomeReusesLoadedAppsAndExplicitInvalidationStillReloads` | Corrección candidata; pendiente CI | Preservar applicationContext aislado de fixture (LauncherReloadTest.kt) |
| 69 | `LauncherReloadTest.profileAppsAndShortcutsNeverEnterThePersonalPreview` | Corrección candidata; pendiente CI | Preservar applicationContext aislado de fixture (LauncherReloadTest.kt) |
| 70 | `LauncherSettingsIntegrationTest.productivityTogglesRemoveAgendaAndBatteryFromTheHomeHeader` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 71 | `LauncherSettingsIntegrationTest.foldersRespectPlacementAndHiddenMembersRemainInPopupAndSearch` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 72 | `LauncherSurfaceTest.drawerReachesThePhysicalTopAndFadesItsAppPixels` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 73 | `ListAppearanceColorTest.dimSwitchRevealsSliderAndColorWhilePreservingTheChoiceWhenDisabled` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 74 | `MediaSessionRepositoryTest.metadataArtworkAndTransportControlsFollowTheRealController` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 75 | `PopupGestureSettingsTest.slowRightSwipeOpensOnTheActualHomePager` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 76 | `PopupWidgetEditingTest.releaseShowsHandlesAndResizeChangesBothWidgetAndPanel` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 77 | `PopupWidgetEditingTest.fullWidthRestoresInsetAndScaleDoesNotChangeOuterSize` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 78 | `ProfileFavoritesTest.combinedGroupHasThreeAddActionsThenDisabledOrSelectableProfileCheckboxes` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 79 | `ProfileFavoritesTest.workFavoriteOpensAPopupOnHomeEvenWhenDrawerModeIsList` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 80 | `ProfileFavoritesTest.privateFavoriteAuthenticatesThenOpensOnHomeEvenWhenDrawerModeIsList` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 81 | `SearchGestureInteractionTest.emptyResultsStillStretchAndPullDownClosesSearch` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 82 | `SearchGestureInteractionTest.shortResultsUseNativeStretchAtBothEdgesWithoutDismissing` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 83 | `SettingsNavigationTest.cancellingBackKeepsFolderDraftAndDirectEditorExitsItsOwnFlow` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 84 | `SettingsRefinementTest.advancedShowsComingSoonAndCanReturnToSettings` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 85 | `TypographyTest.clockDateScheduleRailAndDrawerHeadingsKeepTheLauncherFamily` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 86 | `TypographyTest.detailsAdvancedPackageAndRenameFieldInheritFamilyThroughNestedThemes` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 87 | `WeatherStyleTest.settingsKeepsProviderAndLicenseCreditsAwayFromAgenda` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 88 | `WeatherUiTest.calendarPermissionAndCalendarErrorsKeepTheWeatherAvailable` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 89 | `WeatherUiTest.chineseNarrowAgendaKeepsLocalizedDatesAndWeatherReadable` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |
| 90 | `WeatherUiTest.homeWeatherSharesDateActionAndAgendaMergesForecastWithEvents` | Pendiente; sin corrección certificada | Reproducir con informe de familia; no se altera la aserción |

Producción: el editor conserva expansión por visita/recreación usando rememberSaveable. CI ejecutará familias y suite completa en emuladores separados y secuencialmente; sin compartir datos entre ambos ni eliminar tests. Detalles de fallo originales: comentario 6891772141 de PR #1. Próximo paso: cotejar cada caso con el JSON sanitizado de CI; no inferir resultados de ausencia en un log parcial.

