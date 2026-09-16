# GLB 模型存放目录

把智能体 GLB 文件放在本目录下，首页圆环内即可用 3D 展示。

## 使用方式

1. 将你的 GLB 文件放入此目录，并命名为 **`agent_default.glb`**（首页默认加载该路径）。
2. 若使用其他文件名，请在首页 `pages/home/home.js` 的 `data.glbPath` 中修改为对应路径，例如：
   - `/packageModels/glb/你的文件名.glb`

## 路径说明

- 分包根目录：`packageModels/`（在 `app.json` 的 `subpackages` 中已配置）
- 本目录：`packageModels/glb/`
- 引用示例：`/packageModels/glb/agent_default.glb`

## 注意事项

- 主包有 2MB 限制，较大模型建议放此分包或使用云存储。
- 若未放置 `agent_default.glb`，首页将自动使用 2D 智能体图片（`images/common/agent.png`）作为降级展示。
