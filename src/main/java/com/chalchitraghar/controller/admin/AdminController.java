package com.chalchitraghar.controller.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin test controller endpoint.
 */
@RequestMapping("/api/admin")
@RestController
public class AdminController {

    /**
     * Test endpoint for admin access verification.
     *
     * @return test message
     */
    @GetMapping()
    public String test() {
        return "Hello, world!";
    }

}
