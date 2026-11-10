package com.opsorder.nameless;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.opsorder")
// 说明：本类在 com.opsorder.nameless 包，而 controller / service / config 等组件在平级的
// com.opsorder.controller 等包下。默认扫描范围只有 com.opsorder.nameless.**，
// 会导致这些 Bean 一个都扫不进去（表现为接口全部 404、OpenAPI 文档 paths 为空），
// 因此显式把扫描根放宽到 com.opsorder。
@MapperScan (basePackages = "com.opsorder.mapper") // 扫描 Mapper 接口所在的包
public class NamelessApplication {

	public static void main(String[] args) {
		SpringApplication.run(NamelessApplication.class, args);
	}

}
