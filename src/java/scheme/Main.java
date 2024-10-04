pa ckage scheme;

import arc.graphics.g2d.Draw;
import arc.util.Log;
import arc.util.Tmp;
import mindustry.content.Blocks;
import mindustry.game.Schematics;
import mindustry.gen.Building;
import mindustry.mod.Mod;
import mindustry.mod.Scripts;
import mindustry.type.Item;
import mindustry.ui.CoreItemsDisplay;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.Router;
import mindustry.world.blocks.logic.LogicDisplay;
import scheme.moded.ModedSchematics;
import scheme.tools.MessageQueue;
import scheme.ui.MapResizeFix;

import static arc.Core.*;
import static mindustry.Vars.*;
import static scheme.SchemeVars.*;

public class Main extends Mod {

    public Main() {
        // well, after the 136th build, it became much easier
        maxSchematicSize = 512;

        // mod reimported through mods dialog
        if (schematics.getClass().getSimpleName().startsWith("Moded")) return;

        assets.load(schematics = m_schematics = new ModedSchematics());
        assets.unload(Schematics.class.getSimpleName()); // prevent dual loading
    }

    @Override
    public void init() {
        ClajIntegration.load();
        SchemeVars.load();

        ui.schematics = schemas; // do it before build hudfrag
        ui.listfrag = listfrag;

        units.load();
        builds.load();
        keycomb.load();

        m_settings.apply(); // sometimes settings are not self-applying

        hudfrag.build(ui.hudGroup);
        listfrag.build(ui.hudGroup);
        shortfrag.build(ui.hudGroup);
        consolefrag.build();
        corefrag.build(ui.hudGroup);

        control.setInput(m_input.asHandler());
        renderer.addEnvRenderer(0, render::draw);

        if (m_schematics.requiresDialog) ui.showOkText("@rename.name", "@rename.text", () -> {});
        if (settings.getBool("welcome")) ui.showOkText("@welcome.name", "@welcome.text", () -> {});

        try { // run main.js without the wrapper to access the constant values in the game console
            Scripts scripts = mods.getScripts();
            scripts.context.evaluateReader(scripts.scope, SchemeUpdater.script().reader(), "main.js", 0);
            log("Added constant variables to developer console.");
        } catch (Throwable e) { error(e); }

        Blocks.distributor.buildType = () -> ((Router) Blocks.distributor).new RouterBuild() {
            @Override
            public boolean canControl() { return true; }

        content.blocks().each(block -> block instanceof LogicDisplay, block -> block.buildType = () -> ((LogicDisplay) block).new LogicDisplayBuild() {
            @Override
            public void draw() {
                super.draw();
                if (render.borderless) Draw.draw(Draw.z(), () -> {
                    Draw.rect(Draw.wrap(buffer.getTexture()), x, y, block.region.width * Draw.scl, -block.region.height * Draw.scl);
                });
            }
        });
    }

    public static void log(String info) {
        app.post(() -> Log.infoTag("Scheme", info));
    }

    public static void error(Throwable info) {
        app.post(() -> Log.err("Scheme", info));
    }

    public static void copy(String text) {
        if (text == null) return;

        app.setClipboardText(text);
        ui.showInfoFade("@copied");
    }
}
