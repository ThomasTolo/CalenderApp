package CalenderApp.demo.service;

import CalenderApp.demo.model.AppUser;

public interface CurrentUserService {
    AppUser requireByUsername(String username);
}
