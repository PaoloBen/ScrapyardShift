package model.level;

import model.item.WasteFactory;
import model.item.WasteItem;

public class DynamicFacilityLevel extends FacilityLevel {
    private final int layoutPreset;

    private static final int[][] STAGE_PRESETS = {
            { 80, 350,  230, 350,  380, 350,  535, 350 },  // 0: Bottom row machines, L->R top belt
            { 220, 225, 375, 225,  530, 225,  55, 225 },   // 1: Center island machines, R->L top belt, bottom bins
            { 340, 110, 510, 110,  340, 335,  510, 335 },  // 2: Split quad machines, T->B left vertical belt
            { 65, 90,   65, 210,   65, 330,   235, 330 },  // 3: Left wall machines, B->T center-right vertical belt
            { 240, 82,  430, 82,   240, 355,  430, 355 },  // 4: Top/Bottom reactor symmetry, R->L center belt
            { 70, 225,  225, 225,  380, 225,  540, 225 },  // 5: Mid-floor line, L->R bottom belt, right bins
            { 230, 105, 385, 105,  540, 105,  70, 105 },   // 6: Top row machines, R->L mid belt, bottom bins
            { 255, 120, 255, 250,  425, 120,  425, 250 }   // 7: Center cluster, B->T left vertical belt, right bins
    };

    private static final int[][] BELT_PRESETS = {
            { 80, 120,  500, 120 }, // 0: L->R Top
            { 620, 100, 220, 100 }, // 1: R->L Top
            { 135, 90,  135, 320 }, // 2: T->B Left Vertical
            { 505, 360, 505, 140 }, // 3: B->T Center Vertical
            { 560, 225, 200, 225 }, // 4: R->L Center
            { 80, 355,  500, 355 }, // 5: L->R Bottom
            { 580, 235, 190, 235 }, // 6: R->L Center-Left
            { 115, 355, 115, 125 }  // 7: B->T Left Vertical
    };

    private static final int[][] BAY_PRESETS = {
            { 765, 72,  765, 164,  765, 256,  765, 348 }, // 0: Right Wall
            { 185, 368, 325, 368,  465, 368,  605, 368 }, // 1: Bottom Row
            { 765, 72,  765, 164,  765, 256,  765, 348 }, // 2: Right Wall
            { 765, 72,  765, 164,  765, 256,  765, 348 }, // 3: Right Wall
            { 765, 72,  765, 164,  765, 256,  765, 348 }, // 4: Right Wall
            { 765, 72,  765, 164,  765, 256,  765, 348 }, // 5: Right Wall
            { 185, 368, 325, 368,  465, 368,  605, 368 }, // 6: Bottom Row
            { 765, 72,  765, 164,  765, 256,  765, 348 }  // 7: Right Wall
    };

    public DynamicFacilityLevel(int levelNumber, String title, double baseBeltSpeed, int layoutPreset) {
        super(levelNumber, title, levelNumber * 250, baseBeltSpeed);
        this.layoutPreset = Math.floorMod(layoutPreset, STAGE_PRESETS.length);
    }

    @Override
    public WasteItem spawnItem(double startX, double startY, int lane) {
        int tier = getLevelNumber() < 8 ? 2 : 3;
        return WasteFactory.createItemForTier(tier, startX, startY, lane);
    }

    @Override
    public int[] getStageLayout() {
        return STAGE_PRESETS[layoutPreset];
    }

    @Override
    public int[] getBeltLayout() {
        return BELT_PRESETS[layoutPreset];
    }

    @Override
    public int[] getBayLayout() {
        return BAY_PRESETS[layoutPreset];
    }
}