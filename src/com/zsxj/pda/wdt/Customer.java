package com.zsxj.pda.wdt;

public class Customer {

	public final int customerId;
	public final String nickName;
	public final String customerName;
	public final String tel;
	public final String zip;
	public final String province;
	public final String city;
	public final String district;
	public final String address;
	public final String email;
	
	public Customer(int customerId, String nickName, String customerName,
			String tel, String zip, String province, String city,
			String district, String address, String email) {
		super();
		this.customerId = customerId;
		this.nickName = nickName;
		this.customerName = customerName;
		this.tel = tel;
		this.zip = zip;
		this.province = province;
		this.city = city;
		this.district = district;
		this.address = address;
		this.email = email;
	}
}
