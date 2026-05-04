# CityWalk Pulse 评估系统

基于 RAGAS 的 LLM Agent 评估平台。

## 项目结构

```
citywalk-evaluation/
├── .gitlab-ci.yml       # CI/CD 流水线脚本
├── backend/             # SpringBoot 后端
│   ├── maven-settings.xml   # Maven 国内镜像配置
│   ├── pom.xml
│   └── src/
└── frontend/            # Vue3 前端
    ├── package.json
    └── src/
```

## 后端开发

```bash
cd backend
mvn spring-boot:run
```

## 前端开发

```bash
cd frontend
npm install
npm run dev
```
