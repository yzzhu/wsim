package cn.edu.ustc.wsim.dao;

import java.util.List;

import cn.edu.ustc.wsim.bean.User;

public interface UserDao extends BaseDao {
	
	public List<User> getUsersByName(String name);
	
	public User getUserByEmail(String email);
	
	public User getUserByEmailAndPassoord(String email, String password);
	
	public List<User> searchUserByName(String name);
	
	public Integer getScoreByEmail(String email);
	
	public boolean incScoreByEmail(String email);
}
