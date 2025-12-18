package CalenderApp.demo.service.impl;

import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.repository.AppUserRepository;
import CalenderApp.demo.service.CurrentUserService;
import CalenderApp.demo.service.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserServiceImpl implements CurrentUserService {

    private final AppUserRepository userRepository;

    public CurrentUserServiceImpl(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AppUser requireByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
