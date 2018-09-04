package top.ziweb.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import top.ziweb.pojo.Record;
import top.ziweb.pojo.RecordExample;

public interface RecordMapper {
    int countByExample(RecordExample example);

    int deleteByExample(RecordExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(Record record);

    int insertSelective(Record record);

    List<Record> selectByExample(RecordExample example);

    Record selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") Record record, @Param("example") RecordExample example);

    int updateByExample(@Param("record") Record record, @Param("example") RecordExample example);

    int updateByPrimaryKeySelective(Record record);

    int updateByPrimaryKey(Record record);
}