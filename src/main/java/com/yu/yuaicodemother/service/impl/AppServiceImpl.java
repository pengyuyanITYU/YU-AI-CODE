package com.yu.yuaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yu.yuaicodemother.common.BaseResponse;
import com.yu.yuaicodemother.common.ResultUtils;
import com.yu.yuaicodemother.exception.ErrorCode;
import com.yu.yuaicodemother.exception.ThrowUtils;
import com.yu.yuaicodemother.model.dto.app.AppAddRequest;
import com.yu.yuaicodemother.model.entity.App;
import com.yu.yuaicodemother.mapper.AppMapper;
import com.yu.yuaicodemother.model.entity.User;
import com.yu.yuaicodemother.model.enums.CodeGenTypeEnum;
import com.yu.yuaicodemother.service.AppService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 应用 服务层实现。
 *
 * @author 鱼🐟
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{




}
