/**
 * 应用部署状态枚举
 */
export enum AppDeployStatusEnum {
  NOT_DEPLOYED = 0,
  ONLINE = 1,
  OFFLINE = 2,
}

/**
 * 应用部署状态映射
 */
export const APP_DEPLOY_STATUS_MAP = {
  [AppDeployStatusEnum.NOT_DEPLOYED]: {
    text: '未部署',
    color: 'default',
  },
  [AppDeployStatusEnum.ONLINE]: {
    text: '已上线',
    color: 'success',
  },
  [AppDeployStatusEnum.OFFLINE]: {
    text: '已下线',
    color: 'warning',
  },
};

/**
 * 应用生成状态枚举
 */
export enum AppGenStatusEnum {
  NOT_STARTED = 0,
  GENERATING = 1,
  GENERATED_SUCCESS = 2,
  GENERATED_FAILED = 3,
}

/**
 * 应用生成状态映射
 */
export const APP_GEN_STATUS_MAP = {
  [AppGenStatusEnum.NOT_STARTED]: {
    text: '未开始',
    color: 'default',
  },
  [AppGenStatusEnum.GENERATING]: {
    text: '生成中',
    color: 'processing',
  },
  [AppGenStatusEnum.GENERATED_SUCCESS]: {
    text: '生成成功',
    color: 'success',
  },
  [AppGenStatusEnum.GENERATED_FAILED]: {
    text: '生成失败',
    color: 'error',
  },
};

/**
 * 应用精选状态枚举
 */
export enum AppFeaturedStatusEnum {
  NOT_APPLIED = 0,
  PENDING = 1,
  FEATURED = 2,
  REJECTED = 3,
}

/**
 * 应用精选状态映射
 */
export const APP_FEATURED_STATUS_MAP = {
  [AppFeaturedStatusEnum.NOT_APPLIED]: {
    text: '未申请',
    color: 'default',
  },
  [AppFeaturedStatusEnum.PENDING]: {
    text: '申请中',
    color: 'processing',
  },
  [AppFeaturedStatusEnum.FEATURED]: {
    text: '已精选',
    color: 'gold',
  },
  [AppFeaturedStatusEnum.REJECTED]: {
    text: '已拒绝',
    color: 'error',
  },
};
