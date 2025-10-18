package com.cognitive.aaaa.demo.infra

import com.cognitive.aaaa.demo.domain.model.User
import com.cognitive.aaaa.demo.domain.repository.UserRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Controller
class UIController(
    private val userRepository: UserRepository
) {

    @GetMapping("/")
    fun index(model: Model): String {
        return "greeting"
    }

    @PostMapping
    fun createUser(@RequestBody user: User): String {
        userRepository.save(user)
        return "greeting"
    }

}