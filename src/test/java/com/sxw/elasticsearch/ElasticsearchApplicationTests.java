package com.sxw.elasticsearch;

import com.sxw.elasticsearch.mapper.ExtResultMapper;
import com.sxw.elasticsearch.model.Item;
import com.sxw.elasticsearch.repository.ItemRepository;
import org.apache.commons.lang3.time.DateUtils;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 根据官方文档测试常用的api
 * 文档地址:https://docs.spring.io/spring-data/elasticsearch/docs/current/reference/html/
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ElasticsearchApplicationTests {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchApplicationTests.class);
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;
    @Autowired
    private ItemRepository itemRepository;
    @Resource
    private ExtResultMapper extResultMapper;


    /**
     * @Description:创建索引，会根据Item类的@Document注解信息来创建
     */
    @Test
    public void testCreateIndex() {
        elasticsearchTemplate.createIndex(Item.class);
    }

    /**
     * 索引数据
     */
    @Test
    public void indexItem() {
        Item item = new Item();
        item.setId(1L);
        item.setTitle("MacBook Pro");
        item.setCategory("笔记本电脑");
        item.setBrand("苹果");
        item.setPrice(12999.0);
        item.setImages("https://www.apple.com/mac.png");
        item.setCreateTime(new Date());

        Item item1 = new Item();
        item1.setId(2L);
        item1.setTitle("重构 改善既有代码的设计");
        item1.setCategory("程序设计");
        item1.setBrand("马丁·福勒(Martin Fowler)");
        item1.setPrice(118.00);
        item1.setImages("http://product.dangdang.com/26913154.html");
        item1.setCreateTime(new Date());

        Item item2 = new Item();
        item2.setId(3L);
        item2.setTitle("Python编程 从入门到实践");
        item2.setCategory("Python");
        item2.setBrand("埃里克·马瑟斯（Eric Matthes）");
        item2.setPrice(61.40);
        item2.setImages("http://bang.dangdang.com/books/bestsellers/01.54.00.00.00.00-recent7-0-0-1-1");
        item2.setCreateTime(new Date());

        Item item3 = new Item();
        item3.setId(4L);
        item3.setTitle("统计之美：人工智能时代的科学思维");
        item3.setCategory("数学");
        item3.setBrand("李舰");
        item3.setPrice(56.70);
        item3.setImages("http://product.dangdang.com/26915070.html");
        item3.setCreateTime(new Date());

        Item item4 = new Item();
        item4.setId(5L);
        item4.setTitle("机器学习");
        item4.setCategory("人工智能");
        item4.setBrand("周志华");
        item4.setPrice(61.60);
        item4.setImages("http://product.dangdang.com/23898620.html");
        item4.setCreateTime(new Date());

        itemRepository.index(item);
        itemRepository.index(item1);
        itemRepository.index(item2);
        itemRepository.index(item3);
        itemRepository.index(item4);
    }

    /**
     * 搜索
     */
    @Test
    public void testSearch() {
        List<Item> itemList = itemRepository.findByTitleLike("机器");
        for (Item item : itemList) {
            System.out.println(item.toString());
        }
    }

    /**
     * 返回实体数量
     */
    @Test
    public void testCount() {
        long count = itemRepository.count();
        System.out.println(count);
    }

    /**
     * 查找全部
     */
    @Test
    public void testFindAll() {
        Iterable<Item> items = itemRepository.findAll();
        Iterator<Item> iterator = items.iterator();
        while (iterator.hasNext()) {
            logger.info(iterator.next().toString());
        }
    }

    /**
     * 返回由给定ID标识的实体
     */
    @Test
    public void testFindById() {
        Optional<Item> item = itemRepository.findById(1L);
        logger.info(item.get().toString());
    }

    /**
     * 指示是否存在具有给定ID的实体
     */
    @Test
    public void testExistsById() {
        logger.info(itemRepository.existsById(2L) + "");
    }


    /**
     * 测试分页
     */
    @Test
    public void testPage() {
        // 注意：页数从 0 开始，0 代表第一页
        Page<Item> page = itemRepository.findAll(PageRequest.of(0, 3));
        // 总条数
        logger.info(page.getTotalElements() + "");

        Iterator<Item> iterator = page.iterator();
        while (iterator.hasNext()) {
            logger.info(iterator.next().toString());
        }

        // 总页数
        logger.info(page.getTotalPages() + "");
    }

    /**
     * 区间检索
     */
    @Test
    public void testBetween() {
        List<Item> items = itemRepository.findByPriceBetween(50.0, 70.0);
        for (Item item : items) {
            logger.info(item.toString());
        }
    }

    /**
     * 测试查询
     */
    @Test
    public void testQuery() {
        String keyword = "程序设计";

        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        // 给name字段更高的权重
        queryBuilder.should(QueryBuilders.matchQuery("title", keyword).boost(3));
        // description 默认权重 1
        queryBuilder.should(QueryBuilders.matchQuery("category", keyword));
        // 至少一个should条件满足
        queryBuilder.minimumShouldMatch(1);

        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(queryBuilder)
                .withPageable(PageRequest.of(0, 10))
                .build();
        logger.info("\n search(): searchContent [" + keyword + "] \n DSL  = \n " + searchQuery.getQuery().toString());

        Page<Item> page = itemRepository.search(searchQuery);

        // 总条数
        logger.info(page.getTotalElements() + "");

        Iterator<Item> iterator = page.iterator();
        while (iterator.hasNext()) {
            logger.info(iterator.next().toString());
        }

        // 总页数
        logger.info(page.getTotalPages() + "");
    }

    /**
     * 测试查询高亮
     * 参考文章：https://www.cnblogs.com/vcmq/p/9966693.html
     */
    @Test
    public void testHighlightQuery() {
        String keyword = "程序设计";

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // 至少一个should条件满足
        boolQuery.minimumShouldMatch(1);

        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<span style=\"color:red\">").postTags("</span>"),
                        new HighlightBuilder.Field("brand").preTags("<span style=\"color:red\">").postTags("</span>"))
                .withPageable(PageRequest.of(0, 10));
        // 最佳字段  + 降低除了name之外字段的权重系数
        MatchQueryBuilder nameQuery = QueryBuilders.matchQuery("title", keyword).analyzer("ik_max_word");
        MatchQueryBuilder authorQuery = QueryBuilders.matchQuery("brand", keyword).boost(0.8f);
        DisMaxQueryBuilder disMaxQueryBuilder = QueryBuilders.disMaxQuery().add(nameQuery).add(authorQuery);
        queryBuilder.withQuery(disMaxQueryBuilder);

        NativeSearchQuery searchQuery = queryBuilder.build();
        Page<Item> items = elasticsearchTemplate.queryForPage(searchQuery, Item.class, extResultMapper);

        items.forEach(e -> logger.info("{}", e));
    }

    @Test
    public void search() throws Exception{
        Item item = new Item();
        item.setTitle("美国");
       // item.setCategory("海关执法");
        Integer page = 0;
        Integer size = 100;

        // 校验参数
        if (null == page || page < 0)
            page = 0; // if page is null, page = 0

        if (null == size || size < 0)
            size = 10; // if size is null, size default 10

        // 构造分页对象
        Pageable pageable = PageRequest.of(page, size);

       // BoolQueryBuilder (Elasticsearch Query)
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (!StringUtils.isEmpty(item.getTitle())) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("title", item.getTitle()));
        }

       /* if (!StringUtils.isEmpty(item.getCategory())) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("category", item.getCategory()));
        }
*/


       /* RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("createTime")
                .format("yyyy-MM-dd HH:mm:ss")
                .gte(start)
                .lte(end);*/
        // BoolQueryBuilder (Spring Query)
     /*   boolQueryBuilder.must(QueryBuilders.rangeQuery("createTime").format("yyyy-MM-dd HH:mm:ss").from(start).to(end).includeLower(true)
                .includeUpper(true));
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withPageable(pageable)
                .withQuery(boolQueryBuilder)
                .withSort(SortBuilders.fieldSort("id").order(SortOrder.ASC))
                //.withFilter(rangeQueryBuilder)
                .build();*/
        // page search
        String start = "2020-05-01 11:28:00";
        String end = "2020-07-16 11:28:00";
       if (!StringUtils.isEmpty(start)) {
           boolQueryBuilder.must(QueryBuilders.rangeQuery("createTime").gte(start));
        }
        if (!StringUtils.isEmpty(end)) {
            boolQueryBuilder.must(QueryBuilders.rangeQuery("createTime").lte(end));
        }
        // BoolQueryBuilder (Spring Query)
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withPageable(pageable)
                //.withQuery(QueryBuilders.rangeQuery("createTime").gte(getMills(start)).lte(getMills(end)))
                .withQuery(boolQueryBuilder)
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .build();


        Page<Item> phoneModelPage = elasticsearchTemplate.queryForPage(searchQuery, Item.class);
        List<Item> results = phoneModelPage.getContent();

        System.out.println(results);

    }

   public Date dateToStr(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        //DateFormat是抽象类 ，抽象类不可以直接创建对象，所以我们创建子类的对象
        Date d1 = null;//这个格式必须按照上面给出的格式进行转化否则出错
        try {
            d1 = sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return d1;
    }

    /**
     * @throws
     * @title 批量插入
     * @description
     * @author lc
     * @updateTime 2020/7/14 18:00
     */

    @Test
    public void batchInsert() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<Item> itemList = new ArrayList<>();
        for (int i = 6; i < 1000; i++) {
            Item item = new Item();
            item.setId(Long.valueOf(i));
            item.setTitle(i + "美国对留学生签证“一刀切”并没变！韩国学生：荒唐到无话可说");
            item.setCategory(i + "美国入境和海关执法局网站6日发布通告说，2020年秋季学期的留学生如果仅上网课，将无法取得赴美签证或维持当前签证。7日在常见问题解答中，又针对身在美国境外的留学生签证问题作了进一步说明。按照这一说明，持F和M签证的国际学生如身在美国境外，可以只上网课并保持签证有效状态");
            item.setBrand(i + "李舰");
            item.setPrice(56.70);
            item.setImages("http://product.dangdang.com/26915070.html");
            item.setCreateTime(addDay(sdf.format(new Date()), i));
            itemList.add(item);
        }
        this.bulkIndex(itemList);

    }

    /**
     * @title     Elasticsearch的批处理
     * @description
     * @author lc
     * @updateTime 2020/7/15 16:24
     * @throws
     */

    public void bulkIndex(List<Item> ItemList) {
        int counter = 0;
        try {
            if (!elasticsearchTemplate.indexExists("item")) {
                elasticsearchTemplate.createIndex("docs");
            }
            List<IndexQuery> queries = new ArrayList<>();
            for (Item item : ItemList) {
                IndexQuery indexQuery = new IndexQuery();
                indexQuery.setId(item.getId() + "");
                indexQuery.setObject(item);
                indexQuery.setIndexName("item");
                indexQuery.setType("docs");
                queries.add(indexQuery);
                if (counter % 500 == 0) {
                    elasticsearchTemplate.bulkIndex(queries);
                    queries.clear();
                    System.out.println("bulkIndex counter : " + counter);
                }
                counter++;
            }
            if (queries.size() > 0) {
                elasticsearchTemplate.bulkIndex(queries);
            }
            System.out.println("bulkIndex completed.");
        } catch (Exception e) {
            System.out.println("IndexerService.bulkIndex e;" + e.getMessage());
            throw e;
        }
    }


    /**
     * @title  获取前n天的数据
     * @description 
     * @author lc 
     * @param: s
     * @param: n
     * @updateTime 2020/7/15 16:22 
     * @return: java.util.Date
     * @throws 
     */
    public static Date addDay(String s, int n) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Calendar cd = Calendar.getInstance();
            cd.setTime(sdf.parse(s));
            cd.add(Calendar.DATE, -(n + 1));//增加一天
            return sdf.parse(sdf.format(cd.getTime()));

        } catch (Exception e) {
            return null;
        }
    }
    /**
     * @title getMills(时间字符串转毫秒数)
     * @description
     * @author lc
     * @param: dateTime
     * @updateTime 2020/7/15 15:49
     * @return: long
     * @throws
     */
    public long getMills(String dateTime) throws Exception{
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(dateTime));
        System.out.println(dateTime+":"+calendar.getTimeInMillis());
        return calendar.getTimeInMillis();
    }


    /**
     * @title millsToStr(时间毫秒转时间字符串)
     * @description 
     * @author lc 
     * @param: date
     * @updateTime 2020/7/15 15:50 
     * @return: java.lang.String
     * @throws 
     */
    public String millsToStr(long date){
        Date date1 = new Date(date);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date1);
    }


}
