package com.example.springboot.dao;

import com.example.springboot.entity.SentimentDict;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SentimentDictDao extends tk.mybatis.mapper.common.Mapper<SentimentDict> {

    @Select("SELECT * FROM sentiment_dict WHERE uuid = #{uuid}")
    List<SentimentDict> findByUser(@Param("uuid") String uuid);

    @Select("SELECT * FROM sentiment_dict WHERE uuid = #{uuid} AND word = #{word}")
    SentimentDict findByWord(@Param("uuid") String uuid, @Param("word") String word);

    @Insert("INSERT INTO sentiment_dict(word, sentiment, weight, uuid) VALUES(#{word}, #{sentiment}, #{weight}, #{uuid})")
    void insertWord(SentimentDict dict);

    @Delete("DELETE FROM sentiment_dict WHERE id = #{id}")
    void deleteById(@Param("id") Integer id);
}
