package space.impact.iblueprints.integration.structures

internal enum class BlueprintPlacementResult {
    PLACED,
    UNSUPPORTED_ITEM,
    PROTECTED,
    OCCUPIED,
    MISSING_ITEM,
    REJECTED,
    WRONG_BLOCK,
    EXTRACTION_FAILED,
}
