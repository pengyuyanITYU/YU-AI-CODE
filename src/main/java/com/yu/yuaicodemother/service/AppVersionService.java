package com.yu.yuaicodemother.service;

import com.mybatisflex.core.service.IService;
import com.yu.yuaicodemother.model.entity.AppVersion;
import com.yu.yuaicodemother.model.entity.User;
import com.yu.yuaicodemother.model.vo.app.AppVersionDiffVO;
import com.yu.yuaicodemother.model.vo.app.AppVersionVO;

import java.util.List;

/**
 * 应用版本 服务层。
 *
 * @author 鱼🐟
 */
public interface AppVersionService extends IService<AppVersion> {

    AppVersion createVersion(Long appId, String changeLog);

    List<AppVersionVO> listVersions(Long appId);

    boolean rollbackToVersion(Long appId, Integer version, User loginUser);

    AppVersionDiffVO compareVersions(Long appId, Integer v1, Integer v2);
}
