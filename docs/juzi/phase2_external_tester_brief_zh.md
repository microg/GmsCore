# Phase 2 外部真机测试任务说明（给测试员）

## 测试目标
验证 microG RCS 补丁在真实 `SIM + 运营商 + microG ROM` 场景下的行为，不做主观口头结论，只提交结构化证据。

## 设备要求（至少满足一组）
- 设备：Pixel 6/7 或 Samsung S22/S23（同级可接受）
- Android：13 或 14
- ROM：支持 microG 的 ROM
- SIM：可正常联网，所在地区支持 Google Messages RCS

## 执行步骤
1. 安装补丁版 microG（由我们提供 APK）。
2. 安装或清空 Google Messages 数据。
3. 打开 Messages > RCS 设置，触发初始化流程。
4. 保持 3-10 分钟，观察状态。
5. 导出 logcat（完整原始日志，不要删改）。

## 必交付内容（缺一不可）
1. `raw_logcat.log`（原始日志）
2. `metadata.json`（按下面模板填写）

```json
{
  "tester_id": "t001",
  "device": "Pixel 6",
  "rom": "LineageOS 21 + microG",
  "android": "14",
  "carrier_country": "CarrierName/Country",
  "messages_version": "2026.xx.xx",
  "microg_commit": "PR3294-branch-commit",
  "result_state": "Connected|Setting up|Error",
  "time_to_state_seconds": 0,
  "notes": ""
}
```

## 通过标准（由我们统一判定）
- `Connected` 且日志无重复 rank-1 blocker 行，为通过。
- 若未通过，也必须提交完整日志；失败样本同样有价值。
