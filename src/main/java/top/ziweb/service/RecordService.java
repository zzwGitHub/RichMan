package top.ziweb.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import top.ziweb.mapper.RecordMapper;
import top.ziweb.pojo.Record;
import top.ziweb.pojo.RecordExample;

@Service
public class RecordService {

	@Autowired
	private RecordMapper recordMapper;
	
	public List<Record> list(){
		RecordExample example = new RecordExample();
		List<Record> res = recordMapper.selectByExample(example);
		return res;
	}
}
