package com.tlcsdm.autoTest;

import org.junit.Test;
import org.sikuli.basics.HotkeyEvent;
import org.sikuli.basics.HotkeyListener;
import org.sikuli.basics.Settings;
import org.sikuli.script.Key;
import org.sikuli.script.KeyModifier;
import org.sikuli.script.Sikulix;
import org.sikuli.script.support.RunTime;
import org.sikuli.script.support.Runner;

/**
 * @author: 唐 亮
 * @date: 2022/9/12 19:42
 * @since: 1.0
 */
public class SikulixTest {

    @Test
    public void sikulixTest1() {
        Sikulix.popup("Hello World！\n玩得开心！", "test");
        Sikulix.popError("Uuups，这不起作用");
        Sikulix.popAsk("我们真的应该继续吗？");
        Sikulix.input("请输入您的姓名进行登录：");
        Sikulix.input("请输入您的姓名登录：", "anonymous");
        Sikulix.input("请输入您的密码", true);
    }

    @Test
    public void sikulixTest2() {
        Settings.InputFontMono = true;
        Settings.InputFontSize = 20;
        String story = Sikulix.inputText("请讲一个故事");
        story.split("\n");
    }

    @Test
    public void sikulixTest3() {
        String selected = Sikulix.popSelect("请从列表中选择一个项目", new String[]{"未选择", "item1", "item2", "item3"});
        System.out.println(selected);
        Sikulix.popFile("file");
    }

    @Test
    public void sikulixTest4() {
        Key.addHotkey(Key.F1, KeyModifier.ALT + KeyModifier.CTRL, new HotkeyListener() {
            public void hotkeyPressed(HotkeyEvent e) {
                RunTime.get();
                if (RunTime.runningScripts()) {
                    Runner.abortAll();
                    RunTime.terminate(254, "AbortKey was pressed: aborting all running scripts");
                }
            }
        });
    }
}