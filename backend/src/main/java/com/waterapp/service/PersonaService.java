package com.waterapp.service;

import com.waterapp.entity.User;
import com.waterapp.mapper.UserMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 粥粥人设与话术服务（未完成项清单 - 任务1）
 * 提供 GET /ai/persona 所需数据
 */
@Slf4j
@Service
public class PersonaService {

    @Autowired
    private UserMapper userMapper;

    private static final String DEFAULT_PLACEHOLDER = "今天喝水了吗？💧";

    /**
     * 获取粥粥人设与当前话术
     */
    public PersonaVO getPersona(Long userId) {
        PersonaVO vo = new PersonaVO();
        vo.setName("粥粥");
        vo.setTone("可爱、温柔、鼓励型");
        vo.setStyleNote("短句、亲昵称呼，轻拟声词点缀，禁止命令式与责备");

        String placeholder = DEFAULT_PLACEHOLDER;
        if (userId != null) {
            User user = userMapper.selectById(userId);
            if (user != null && user.getCoverText() != null && !user.getCoverText().trim().isEmpty()) {
                placeholder = user.getCoverText().trim();
            }
        }
        vo.setPlaceholder(placeholder);

        List<PhraseItem> phrases = new ArrayList<>();
        phrases.add(new PhraseItem("GREETING", "嗨～我是粥粥🌱 今天也一起把水喝得亮晶晶吧！"));
        phrases.add(new PhraseItem("REMIND_SOFT", "粥粥轻轻敲敲杯子～咕嘟一口水好吗？💧"));
        phrases.add(new PhraseItem("AFTER_CHECKIN", "叮咚！你喝了水～粥粥给你贴贴夸夸！"));
        vo.setPhrases(phrases);

        return vo;
    }

    @Data
    public static class PersonaVO {
        private String name;
        private String tone;
        private String styleNote;
        private String placeholder;
        private List<PhraseItem> phrases;
    }

    @Data
    public static class PhraseItem {
        private String scene;
        private String text;

        public PhraseItem(String scene, String text) {
            this.scene = scene;
            this.text = text;
        }
    }
}
