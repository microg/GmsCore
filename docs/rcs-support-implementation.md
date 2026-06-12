# RCS Support 实现文档

## 概述

RCS（Rich Communication Services）是GSMA定义的下一代消息协议，旨在替代传统SMS/MMS，提供类似OTT应用的丰富通信功能。本任务目标是在现有消息系统中集成RCS支持，使应用能够发送/接收富媒体消息、群聊、文件传输、位置共享等。实现基于GSMA RCS Universal Profile 2.4+规范，兼容主流运营商网络。

## 详细说明

### 1. 核心架构

RCS支持分为三个层次：

- **传输层**：基于SIP/IMS协议栈，通过MSRP（Message Session Relay Protocol）传输媒体内容，HTTP/HTTPS用于文件传输
- **会话层**：管理消息会话状态，支持一对一、群组会话
- **应用层**：提供消息格式化、富媒体处理、业务逻辑

### 2. 关键组件

```
┌─────────────────────────────────────┐
│          Application Layer          │
│  ┌─────────┐ ┌─────────┐ ┌──────┐  │
│  │Message  │ │Group    │ │File  │  │
│  │Handler  │ │Manager  │ │Share │  │
│  └────┬────┘ └────┬────┘ └──┬───┘  │
├───────┴───────────┴─────────┴──────┤
│         Session Layer               │
│  ┌──────────────────────────────┐   │
│  │  SIP/IMS Stack               │   │
│  └──────────────┬───────────────┘   │
├─────────────────┴───────────────────┤
│         Transport Layer              │
│  ┌─────────┐ ┌─────────┐ ┌──────┐  │
│  │MSRP     │ │HTTP/HTTPS│ │Web- │  │
│  │         │ │         │ │Socket│  │
│  └─────────┘ └─────────┘ └──────┘  │
└─────────────────────────────────────┘
```

### 3. 协议实现要点

#### 3.1 SIP注册与认证

```python
# RCS SIP注册示例
import asyncio
from rcs_sdk import SIPClient, RCSConfig

async def register_rcs():
    config = RCSConfig(
        ims_uri="sip:user@ims.mnc001.mcc310.pub.3gppnetwork.org",
        password="auth_password",
        realm="ims.mnc001.mcc310.pub.3gppnetwork.org",
        proxy="sip:proxy.ims.operator.com:5060"
    )
    
    client = SIPClient(config)
    
    # 注册流程
    await client.register(
        expires=3600,  # 注册有效期
        contact="sip:user@192.168.1.100:5060;transport=tcp",
        user_agent="RCSApp/1.0"
    )
    
    # 订阅注册状态
    @client.on("registration_state")
    async def on_registration(state):
        if state == "REGISTERED":
            print("RCS注册成功")
            # 开始消息监听
            await client.start_message_listener()
        elif state == "FAILED":
            print("注册失败，尝试重新注册")
            await client.register()
    
    await client.run()
```

#### 3.2 消息发送与接收

```javascript
// RCS消息发送示例 (Node.js)
const { RCSClient, MessageType } = require('rcs-sdk');

const client = new RCSClient({
    imsEndpoint: 'sip:proxy.ims.operator.com:5060',
    clientId: 'user@domain.com',
    capabilities: {
        fileTransfer: true,
        groupChat: true,
        geolocation: true,
        richCard: true
    }
});

// 发送文本消息
async function sendTextMessage(to, text) {
    const message = {
        type: MessageType.TEXT,
        content: text,
        contentType: 'text/plain',
        charset: 'UTF-8'
    };
    
    const result = await client.sendMessage(to, message);
    console.log('消息ID:', result.messageId);
    return result;
}

// 发送富媒体消息
async function sendRichCard(to, cardData) {
    const message = {
        type: MessageType.RICH_CARD,
        content: {
            title: "商品推荐",
            description: "限时优惠",
            media: {
                type: "image",
                url: "https://cdn.example.com/promo.jpg",
                thumbnail: "https://cdn.example.com/thumb.jpg"
            },
            suggestions: [
                {
                    action: "open_url",
                    label: "立即购买",
                    url: "https://shop.example.com/deal"
                },
                {
                    action: "reply",
                    label: "了解更多",
                    postback: "INFO_REQUEST"
                }
            ]
        }
    };
    
    return await client.sendMessage(to, message);
}

// 文件传输
async function sendFile(to, filePath) {
    const fileInfo = await client.prepareFileTransfer(filePath);
    
    const message = {
        type: MessageType.FILE_TRANSFER,
        content: {
            fileName: fileInfo.name,
            fileSize: fileInfo.size,
            fileUrl: fileInfo.url,
            fileType: fileInfo.mimeType,
            thumbnail: fileInfo.thumbnailUrl
        }
    };
    
    return await client.sendMessage(to, message);
}
```

#### 3.3 群组管理

```python
# RCS群组管理示例
class RCSGroupManager:
    def __init__(self, rcs_client):
        self.client = rcs_client
        self.groups = {}
    
    async def create_group(self, name, members, icon=None):
        """创建RCS群组"""
        group_config = {
            "subject": name,
            "participants": members,
            "icon": icon,
            "capabilities": {
                "maxParticipants": 100,
                "maxFileSize": 10485760,  # 10MB
                "supportedTypes": ["text", "image", "video", "file"]
            }
        }
        
        group = await self.client.create_group(group_config)
        self.groups[group.id] = group
        return group
    
    async def add_members(self, group_id, new_members):
        """添加群成员"""
        group = self.groups.get(group_id)
        if not group:
            raise ValueError("群组不存在")
        
        # 发送SIP REFER请求添加成员
        result = await self.client.group_operation(
            group_id,
            "add_participants",
            participants=new_members
        )
        
        # 发送群组更新通知
        await self.client.send_group_notification(
            group_id,
            "participants_added",
            data={"new_members": new_members}
        )
        
        return result
    
    async def send_group_message(self, group_id, message):
        """发送群消息"""
        # 使用MSRP发送群消息
        session = await self.client.create_msrp_session(
            group_id,
            message_type="group_chat"
        )
        
        await session.send(message)
        await session.close()
    
    async def handle_group_event(self, event):
        """处理群组事件"""
        if event.type == "participant_joined":
            await self._notify_join(event)
        elif event.type == "participant_left":
            await self._notify_leave(event)
        elif event.type == "subject_changed":
            await self._update_subject(event)
```

### 4. 配置示例

```yaml
# RCS配置文件 rcs-config.yaml
rcs:
  # IMS配置
  ims:
    home_domain: "ims.operator.com"
    pcscf_address: "sip:pcscf.ims.operator.com:5060"
    transport: "tcp"
    authentication:
      method: "digest"
      realm: "ims.mnc001.mcc310.pub.3gppnetwork.org"
  
  # 功能配置
  capabilities:
    max_message_size: 10485760  # 10MB
    max_group_size: 100
    supported_media_types:
      - "image/jpeg"
      - "image/png"
      - "video/mp4"
      - "audio/aac"
      - "application/pdf"
    file_transfer:
      max_file_size: 52428800  # 50MB
      chunk_size: 65536  # 64KB
      concurrent_transfers: 3
  
  # 存储配置
  storage:
    message_history: 30  # 保留天数
    media_cache_size: 500  # MB
    database:
      type: "postgresql"
      connection: "postgresql://user:pass@localhost:5432/rcs_db"
  
  # 网络配置
  network:
    retry_interval: 5  # 秒
    max_retries: 3
    timeout:
      connection: 30
      message_delivery: 120
      file_transfer: 300
```

### 5. 错误处理与调试

```python
# RCS错误处理示例
class RCSErrorHandler:
    ERROR_CODES = {
        400: "Bad Request - 消息格式错误",
        401: "Unauthorized - 认证失败",
        403: "Forbidden - 无权限",
        404: "Not Found - 用户不存在",
        408: "Request Timeout - 请求超时",
        413: "Message Too Large - 消息过大",
        415: "Unsupported Media Type - 不支持的媒体类型",
        486: "Busy Here - 用户忙",
        488: "Not Acceptable - 不接受当前媒体",
        500: "Server Internal Error - 服务器错误",
        503: "Service Unavailable - 服务不可用",
        603: "Decline - 用户拒绝"
    }
    
    def handle_sip_error(self, sip_response):
        """处理SIP错误响应"""
        code = sip_response.status_code
        error_info = {
            "code": code,
            "message": self.ERROR_CODES.get(code, "未知错误"),
            "detail": sip_response.reason_phrase,
            "headers": dict(sip_response.headers)
        }
        
        # 记录错误日志
        self._log_error(error_info)
        
        # 根据错误类型处理
        if code in [401, 403]:
            return self._handle_auth_error(sip_response)
        elif code == 413:
            return self._handle_message_too_large(sip_response)
        elif code == 503:
            return self._handle_service_unavailable(sip_response)
        else:
            return self._handle_generic_error(sip_response)
    
    def _handle_auth_error(self, response):
        """处理认证错误 - 尝试重新认证"""
        auth_header = response.headers.get("WWW-Authenticate")
        if auth_header:
            # 解析认证挑战
            challenge = self._parse_auth_challenge(auth_header)
            # 重新计算认证信息
            new_auth = self._calculate_digest(challenge)
            return {"action": "retry_auth", "auth": new_auth}
        return {"action": "fail"}
    
    def _handle_message_too_large(self, response):
        """处理消息过大 - 分片发送"""
        max_size = int(response.headers.get("Content-Length", 0))
        return {
            "action": "split_message",
            "max_chunk_size": max_size - 1024  # 预留头部空间
        }
    
    def _log_error(self, error_info):
        """记录错误到日志系统"""
        import logging
        logger = logging.getLogger("rcs.error")
        logger.error(
            f"RCS Error: {error_info['code']} - {error_info['message']}",
            extra={"error_detail": error_info}
        )
```

## 示例

### 完整消息发送流程

```python
# RCS消息发送完整示例
import asyncio
from rcs_sdk import RCSClient, MessageBuilder

async def demo_rcs_messaging():
    # 初始化客户端
    client = RCSClient.from_config("rcs-config.yaml")
    
    # 1. 注册到IMS网络
    await client.register()
    
    # 2. 获取用户能力
    user_capabilities = await client.get_capabilities("+1234567890")
    print(f"用户支持: {user_capabilities}")
    
    # 3. 构建富媒体消息
    message = MessageBuilder() \
        .add_text("您好！这是RCS消息测试") \
        .add_image("https://example.com/image.jpg", "测试图片") \
        .add_suggestion("打开链接", "https://example.com") \
        .add_suggestion("快速回复", "收到") \
        .build()
    
    # 4. 发送消息
    result = await client.send_message("+1234567890", message)
    
    # 5. 监听送达状态
    @client.on("message_status")
    async def on_status(status):
        if status.message_id == result.message_id:
            print(f"消息状态: {status.state}")
            if status.state == "delivered":
                print("消息已送达")
            elif status.state == "read":
                print("消息已读")
    
    # 6. 保持连接
    await client.wait_for_shutdown()

# 运行
asyncio.run(demo_rcs_messaging())
```

## 注意事项

### 1. 运营商兼容性
- 不同运营商对RCS Universal Profile的支持程度不同，需进行兼容性测试
- 某些运营商可能限制文件大小（通常5-50MB）
- 群组大小限制因运营商而异（通常50-100人）

### 2. 安全考虑
- 所有SIP