package de.jonashackt.springbootvuejs.controller;

import de.jonashackt.springbootvuejs.domain.ScrapeInfo;
import de.jonashackt.springbootvuejs.domain.User;
import de.jonashackt.springbootvuejs.repository.UserRepository;
import de.jonashackt.springbootvuejs.service.YoutubeService;
import de.jonashackt.springbootvuejs.utils.JsonUtil;
import de.jonashackt.springbootvuejs.utils.MD5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/api")
public class BackendController {

    private static final Logger LOG = LoggerFactory.getLogger(BackendController.class);

    public static final String HELLO_TEXT = "Hello from Spring Boot Backend!";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private YoutubeService youtubeService;

    @RequestMapping(path = "/hello")
    public @ResponseBody
    String sayHello() {
        LOG.info("GET called on /hello resource");
        return HELLO_TEXT;
    }

    @RequestMapping(path = "/user", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public @ResponseBody
    long addNewUser(@RequestParam String firstName, @RequestParam String lastName) {
        User user = new User(firstName, lastName);
        userRepository.save(user);

        LOG.info(user.toString() + " successfully saved into DB");

        return user.getId();
    }

    @GetMapping(path = "/user/{id}")
    public @ResponseBody
    User getUserById(@PathVariable("id") long id) {
        LOG.info("Reading user with id " + id + " from database.");
        return userRepository.findById(id).get();
    }

    @GetMapping(path = "/md5")
    public @ResponseBody
    String getMD5(String password, Integer count) {
        LOG.info("MD5 password:{} count:{}", password, count);
        if (password == null) {
            return "Password is empty!";
        }
        String result = password;
        for (int i = 0; i < count; i++) {
            result = MD5Util.encode32(result);
        }
        return result;
    }

    @PostMapping(path = "json")
    public @ResponseBody
    String json(String source) {
        LOG.info("JSON source:{}", source);
        if (source == null) {
            return "Json source is empty";
        }
        try {
            return "'" + JsonUtil.beautifyJson(source);
        } catch (Throwable t) {
            return t.getMessage();
        }
    }

    @PostMapping(path = "youtube")
    public @ResponseBody
    String youtube(String source) {
        LOG.info("Youtube source:{}", source);
        if (source == null) {
            return "Youtube source is empty";
        }
        try {
            ScrapeInfo scrapeInfo = youtubeService.parseWeb(source);
            return JsonUtil.toJsonStr(scrapeInfo);
        } catch (Throwable t) {
            return t.getMessage();
        }
    }
}
