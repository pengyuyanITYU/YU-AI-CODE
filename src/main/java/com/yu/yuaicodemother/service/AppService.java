package com.yu.yuaicodemother.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yu.yuaicodemother.model.dto.app.AppQueryRequest;
import com.yu.yuaicodemother.model.entity.App;
import com.yu.yuaicodemother.model.entity.User;
import com.yu.yuaicodemother.model.vo.app.AppVO;
import reactor.core.publisher.Flux;

import java.io.Serializable;
import java.util.List;

/**
 * 应用 服务层。
 *
 * @author 鱼🐟
 */
public interface AppService extends IService<App> {


    AppVO getAppVO(App app);

    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    List<AppVO> getAppVOList(List<App> appList);

    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    String deployApp(Long appId, User loginUser);

    boolean removeById(Serializable id);

    void generateAppScreenshotAsync(Long appId, String appUrl);
}
