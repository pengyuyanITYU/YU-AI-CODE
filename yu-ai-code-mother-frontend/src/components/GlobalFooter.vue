<template>
  <footer class="global-footer">
    <!-- 背景光效装饰层 -->
    <div class="footer-decoration"></div>

    <div class="footer-content">
<!--      &lt;!&ndash; 品牌/Logo 区域 &ndash;&gt;-->
<!--      <div class="brand-badge">-->
<!--        <span class="icon-wrapper">🐟</span>-->
<!--        <span class="brand-name">鱼跃</span>-->
<!--      </div>-->

      <!-- 版权信息 -->
      <div class="copyright-wrapper">
        <span class="year-badge">{{ currentYear }}</span>
        <span class="divider">/</span>
        <span class="text-prefix">Designed by</span>
        <a
          href="https://github.com/pengyuyanITYU/YU-AI-CODE.git"
          target="_blank"
          rel="noopener noreferrer"
          class="author-link"
        >
          <span class="link-text">鱼</span>
          <span class="link-glow"></span>
        </a>
      </div>

      <!-- 装饰性标语 -->
      <div class="motto">Crafted with liquid inspiration</div>
    </div>
  </footer>
</template>

<script setup lang="ts">
// 自动获取当前年份
const currentYear = new Date().getFullYear();
</script>

<style scoped>
.global-footer {
  width: 100%;
  padding: 60px 0 40px;
  position: relative;
  overflow: hidden;
  border-top: 1px solid rgba(148, 163, 184, 0.15);
  background: linear-gradient(
    180deg,
    rgba(15, 23, 42, 0.85) 0%,
    rgba(30, 41, 59, 0.75) 100%
  );
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

/* 底部发光装饰 */
.footer-decoration {
  position: absolute;
  bottom: -50%;
  left: 50%;
  transform: translateX(-50%);
  width: 60%;
  height: 100px;
  background: radial-gradient(ellipse at center, rgba(59, 130, 246, 0.2) 0%, transparent 70%);
  filter: blur(40px);
  z-index: 0;
  pointer-events: none;
}

.footer-content {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

/* 品牌胶囊 */
.brand-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.05); /* 调暗背景 */
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
  cursor: default;
}

.brand-badge:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.1); /* hover时稍亮 */
  border-color: rgba(148, 163, 184, 0.3);
}

.icon-wrapper {
  font-size: 18px;
  filter: drop-shadow(0 2px 4px rgba(59, 130, 246, 0.2));
  animation: float 3s ease-in-out infinite;
}

.brand-name {
  font-size: 14px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.95); /* 亮色文字 */
  letter-spacing: 0.5px;
}

/* 版权行 */
.copyright-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 13px;
  color: rgba(148, 163, 184, 0.9);
}

.year-badge {
  font-weight: 600;
  color: rgba(148, 163, 184, 0.8);
}

.divider {
  color: rgba(148, 163, 184, 0.5);
  font-size: 10px;
}

/* 链接特效 */
.author-link {
  position: relative;
  text-decoration: none;
  font-weight: 700;
  color: rgba(59, 130, 246, 0.9);
  padding: 2px 6px;
  border-radius: 6px;
  transition: all 0.3s ease;
  overflow: hidden;
}

.link-text {
  position: relative;
  z-index: 2;
}

.link-glow {
  position: absolute;
  top: 0;
  left: 0;
  width: 0%;
  height: 100%;
  background: rgba(59, 130, 246, 0.1);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1;
}

.author-link:hover {
  color: rgba(59, 130, 246, 1);
  letter-spacing: 0.5px;
}

.author-link:hover .link-glow {
  width: 100%;
}

/* 底部格言 */
.motto {
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 2px;
  color: rgba(148, 163, 184, 0.7);
  opacity: 0.8;
  margin-top: 8px;
}

/* 动画定义 */
@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-3px) rotate(5deg); }
}

/* 响应式 */
@media (max-width: 640px) {
  .global-footer {
    padding: 40px 16px 30px;
  }

  .copyright-wrapper {
    flex-direction: column;
    gap: 4px;
  }

  .divider {
    display: none;
  }
}
</style>
