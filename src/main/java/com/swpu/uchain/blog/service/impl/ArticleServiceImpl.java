package com.swpu.uchain.blog.service.impl;

import com.github.pagehelper.PageHelper;
import com.swpu.uchain.blog.dao.ArticleMapper;
import com.swpu.uchain.blog.entity.Article;
import com.swpu.uchain.blog.entity.User;
import com.swpu.uchain.blog.enums.ResultEnum;
import com.swpu.uchain.blog.exception.GlobalException;
import com.swpu.uchain.blog.redis.RedisService;
import com.swpu.uchain.blog.redis.key.ArticleKey;
import com.swpu.uchain.blog.service.ArticleService;
import com.swpu.uchain.blog.service.UserService;
import com.swpu.uchain.blog.util.ResultVOUtil;
import com.swpu.uchain.blog.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName ArticleServiceImpl
 * @Author hobo
 * @Date 19-4-23 下午6:45
 * @Description
 **/
@Service
@Slf4j
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private RedisService redisService;

    @Override
    public boolean insert(Article article) {
        if (articleMapper.insert(article) == 1) {
            redisService.set(ArticleKey.articleKey, article.getTitle(), article);
            return true;
        }
        return false;
    }

    @Override
    public boolean update(Article article) {
        if (articleMapper.updateByPrimaryKey(article) == 1) {
            redisService.set(ArticleKey.articleKey, article.getTitle(), article);
            return true;
        }
        return false;
    }

    @Override
    public boolean delete(Long id) {
        redisService.delete(ArticleKey.articleKey, id + "");
        return (articleMapper.deleteByPrimaryKey(id) == 1);
    }

    public Article findArticleByTitle(String title) {
        return articleMapper.selectByArticleTitle(title);
    }

    @Override
    public ResultVO insertArticle(Article article) {
        if (findArticleByTitle(article.getTitle()) != null) {
            return ResultVOUtil.error(ResultEnum.ARTICLE_TITLE_EXIST);
        }
        if (insert(article)) {
            return ResultVOUtil.success(article);
        }
        return ResultVOUtil.error(ResultEnum.SERVER_ERROR);
    }

    @Override
    public ResultVO updateArticle(Article article) {
        if (findArticleByTitle(article.getTitle()) == null) {
            return ResultVOUtil.error(ResultEnum.ARTICLE_NOT_EXIST);
        }
        if (update(article)) {
            return ResultVOUtil.success(article);
        }
        return ResultVOUtil.error(ResultEnum.SERVER_ERROR);
    }

    @Override
    public ResultVO deleteArticle(Long id) {
        if (articleMapper.selectByPrimaryKey(id) == null) {
            return ResultVOUtil.error(ResultEnum.ARTICLE_NOT_EXIST);
        }
        if (delete(id)) {
            return ResultVOUtil.success();
        }
        return ResultVOUtil.error(ResultEnum.SERVER_ERROR);
    }

    @Override
    public ResultVO selectAll(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Article> list = articleMapper.selectAll();
        return ResultVOUtil.success(list);
    }

    @Override
    public void addReading(Long id) {
        Article article = articleMapper.selectByPrimaryKey(id);
        if (article == null) {
            throw new GlobalException(ResultEnum.ARTICLE_NOT_EXIST);
        }
        //判断读取文章的是否作者本人
        User user = userService.getCurrentUser();
        if (user.getUsername().equals(article.getAuthor())) {
            return;
        }
        article.setReading(article.getReading() + 1);
        if (!update(article)) {
            throw new GlobalException(ResultEnum.ADD_READINGS_ERROR);
        }
    }
}