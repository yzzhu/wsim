package cn.edu.ustc.wsim.websocket.user;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.WsOutbound;

import cn.edu.ustc.wsim.bean.FriendRequest;
import cn.edu.ustc.wsim.bean.Group;
import cn.edu.ustc.wsim.bean.GroupRequest;
import cn.edu.ustc.wsim.bean.Message;
import cn.edu.ustc.wsim.bean.User;
import cn.edu.ustc.wsim.datastructure.OnlineUsers;
import cn.edu.ustc.wsim.service.FriendRequestService;
import cn.edu.ustc.wsim.service.GroupRequestService;
import cn.edu.ustc.wsim.service.MessageService;
import cn.edu.ustc.wsim.util.SpringUtil;


public class UserMessageInbound extends MessageInbound {

	private Integer userId;
	
	public Integer getUserId() {
		return userId;
	}
	
	
	public UserMessageInbound(Integer userId) {
		this.userId = userId;
	}


	//建立websocket连接的触发的事件
	@Override
	protected void onOpen(WsOutbound outbound) {
		//向连接池添加当前的连接对象
		UserMessageInboundPool.addMessageInbound(this);
		
//		UserService us = (UserService) SpringUtil.getBean("userServiceProxy");
//		System.out.println(us.count());
		
		User user = new User(this.userId);
		//websocket链接创建成功，将未处理的好友请求信息，离线消息，等信息发送给用户
		
		//将未处理的好友请求信息发送给该用户
		FriendRequestService friendRequestService = (FriendRequestService) SpringUtil.getBean("friendRequestServiceProxy");
		List<FriendRequest> friendRequests = friendRequestService.getUndealFriendRequests(user);
		for (FriendRequest friendRequest : friendRequests) {
			UserMessageInboundPool.sendFriendRequestMessage(friendRequest);
		}
		
		//将未处理的好友请求信息发送给该用户
		GroupRequestService groupRequestService = (GroupRequestService) SpringUtil.getBean("groupRequestServiceProxy"); 
		Map<Group, List<GroupRequest>> map = groupRequestService.getUndealGroupRequests(user);
		for(Map.Entry mapEntry : map.entrySet()) {  
		    Group group = (Group) mapEntry.getKey();  
		    List<GroupRequest> groupRequests = (List<GroupRequest>) mapEntry.getValue();
//		    if( !(groupRequests == null || groupRequests.size() == 0) )
		    for (GroupRequest groupRequest : groupRequests) {
				UserMessageInboundPool.sendGroupRequestMessage(user, groupRequest);
			}	
		}  
			
		//获取离线消息发送给该用户
		MessageService messageService = (MessageService) SpringUtil.getBean("messageServiceProxy");
		UserMessageInboundPool.sendUnreadMessages(user, messageService.getUnreadMessagesOfUser(user));
		
		
	}

	@Override
	protected void onClose(int status) {
		// 触发关闭事件，在连接池中移除连接
		UserMessageInboundPool.removeMessageInbound(this);
	}

	@Override
	protected void onBinaryMessage(ByteBuffer message) throws IOException {
		throw new UnsupportedOperationException("Binary message not supported.");
	}

	//客户端发送消息到服务器时触发事件
	@Override
	protected void onTextMessage(CharBuffer message) throws IOException {
		String msg = message.toString();
		
		JSONObject json = JSONObject.fromObject(msg);
		String type = (String) json.get("type");
		
		switch(type) {
//		case "dealFriendRequest" :		//可以使用ajax实现
			
//		case "dealGroupRequest" :
			
		case "friendMessage" :
			dealFriendMessage(json, msg);
			break;
			
		case "groupMessage" :
			Integer groupId = (Integer) json.get("groupId");
			UserMessageInboundPool.sendGroupMessage(groupId, msg);
			return;
			
		default :
			return;
		}
		
//		UserMessageInboundPool.sendMessage(userId, msg);
	}




	private void dealFriendMessage(JSONObject json, String msg) {
		Integer receiverId = (Integer) json.get("receiver");
		//将消息进行分析，存入数据库
		Message messageBean = new Message();
		messageBean.setSender(new User(userId));
		messageBean.setReceiver(new User(receiverId));
		messageBean.setTime(new Date());
		
		if(OnlineUsers.isLogin(receiverId)) {
			messageBean.setReaded(true);
			UserMessageInboundPool.sendFriendMessage(receiverId, msg);
		} else {
			messageBean.setReaded(false);
		}
		
		//存入数据库
		MessageService messageService = (MessageService) SpringUtil.getBean("messageServiceProxy");
		messageService.add(messageBean);
		return;
	}
	
	
}
